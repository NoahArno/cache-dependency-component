package top.noaharno.cachedependency.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.data.redis.core.StringRedisTemplate;
import top.noaharno.cachedependency.config.CacheDependencyProperties;
import top.noaharno.cachedependency.constant.CacheLevelEnum;
import top.noaharno.cachedependency.service.CacheDependencyService;
import top.noaharno.cachedependency.util.SqlAnalysisUtil;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 表分析拦截器
 * <p/>
 * 拦截所有执行的 UPDATE SQL，解析出它的表名，并且清空对应的 Redis 缓存依赖关系
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
@Slf4j
public class TableAnalysisInterceptor implements Interceptor {

    private final StringRedisTemplate stringRedisTemplate;

    private final CacheDependencyService cacheDependencyService;

    /**
     * 用于控制缓存删除任务的线程池
     */
    private final ScheduledExecutorService scheduledExecutorService;

    private final CacheDependencyProperties properties;

    /**
     * 用于跟踪正在处理的表删除任务
     */
    private final Map<String, ScheduledFuture<?>> pendingCleanupTasks = new ConcurrentHashMap<>();

    public TableAnalysisInterceptor(StringRedisTemplate stringRedisTemplate,
                                    CacheDependencyService cacheDependencyService,
                                    CacheDependencyProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.cacheDependencyService = cacheDependencyService;
        this.properties = properties;
        // 初始化线程池
        this.scheduledExecutorService = Executors.newScheduledThreadPool(properties.getCleanThreadPoolSize());
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        String id = ms.getId();
        String sql = ms.getSqlSource().getBoundSql(invocation.getArgs()[1]).getSql();
        // 获取 SQL 中涉及的表
        Set<String> tableSet = SqlAnalysisUtil.getTableSet(id, sql);
        // 清空缓存依赖关系
        cleanCacheDependency(tableSet);
        return invocation.proceed();
    }


    /**
     * 清空缓存依赖关系
     *
     * @param tableSet 被清空的表集合
     */
    private void cleanCacheDependency(Set<String> tableSet) {
        try {
            // 创建两个任务列表：一个是立即执行的（秒级新鲜度），另一个是延迟执行的（其他级别）
            List<CacheCleanupTask> immediateTasks = new ArrayList<>();
            PriorityQueue<CacheCleanupTask> delayedTasks = new PriorityQueue<>();

            // 为每个表创建清理任务
            for (String table : tableSet) {
                if (!properties.getTables().contains(table)) {
                    // 不包含该表，表明该表不存在缓存依赖关系，直接跳过，可以避免频繁访问 redis
                    continue;
                }

                // 取消该表之前的清理任务（如果有的话）
                ScheduledFuture<?> previousTask = pendingCleanupTasks.remove(table);
                if (previousTask != null && !previousTask.isDone()) {
                    previousTask.cancel(false);
                }

                // 自增版本号
                long incrementedVersion = stringRedisTemplate.opsForValue().increment(cacheDependencyService.getVersionKey(table));
                if (incrementedVersion == 1) {
                    // 版本号为 1 的时候，表示一开始缓存里面没有任何依赖关系，不需要进行任何处理
                    continue;
                }
                Long previousVersion = incrementedVersion - 1;

                // 为每个新鲜度级别创建任务
                for (CacheLevelEnum cacheLevel : CacheLevelEnum.getSortedValues()) {
                    // 获取到当前缓存级别的缓存依赖关系
                    Set<String> sortedMembers = stringRedisTemplate.opsForZSet().rangeByScore(
                            cacheDependencyService.getDependencyKey(table, String.valueOf(previousVersion)),
                            cacheLevel.getLevel(),
                            cacheLevel.getLevel()
                    );
                    // 如果没有依赖关系，则跳过，避免占用优先级队列
                    if (sortedMembers.isEmpty()) {
                        continue;
                    }
                    CacheCleanupTask task = new CacheCleanupTask(table, previousVersion, sortedMembers, cacheLevel.getLevel());
                    // 秒级新鲜度（级别为1）立即执行，其他级别延迟执行
                    if (cacheLevel.getLevel() == CacheLevelEnum.SECONDS.getLevel()) {
                        immediateTasks.add(task);
                    } else {
                        delayedTasks.add(task);
                    }
                }
                // 处理完后，删除上一个版本的依赖关系
                stringRedisTemplate.delete(cacheDependencyService.getDependencyKey(table, String.valueOf(previousVersion)));
            }

            // 立即执行秒级新鲜度的清理任务
            executeImmediateTasks(immediateTasks);

            // 提交其他级别的任务到调度器，按优先级顺序执行
            scheduleDelayedTasks(delayedTasks);
        } catch (Exception e) {
            // 报错不抛出，不阻断正常业务执行
            log.warn("清空缓存依赖关系失败：{}，所涉及的表为：{}", e.getMessage(), tableSet, e);
        }
    }

    /**
     * 立即执行秒级新鲜度的清理任务
     *
     * @param immediateTasks 立即执行的任务列表
     */
    private void executeImmediateTasks(List<CacheCleanupTask> immediateTasks) {
        for (CacheCleanupTask task : immediateTasks) {
            try {
                doCleanCacheDependencyByFreshness(task.sortedMembers, task.cacheLevel);
            } catch (Exception e) {
                log.error("立即清理缓存依赖关系失败: table={}, version={}, level={}",
                        task.table, task.previousVersion, task.cacheLevel, e);
            }
        }
    }

    /**
     * 调度延迟执行的缓存清理任务
     *
     * @param delayedTasks 延迟执行的任务队列
     */
    private void scheduleDelayedTasks(PriorityQueue<CacheCleanupTask> delayedTasks) {
        // 初始延迟为0
        long delay = 0;
        AtomicInteger submittedTasks = new AtomicInteger(0);

        while (!delayedTasks.isEmpty()) {
            CacheCleanupTask task = delayedTasks.poll();
            ScheduledFuture<?> future = scheduledExecutorService.schedule(() -> {
                try {
                    doCleanCacheDependencyByFreshness(task.sortedMembers, task.cacheLevel);
                } catch (Exception e) {
                    log.error("延迟清理缓存依赖关系失败: table={}, version={}, level={}",
                            task.table, task.previousVersion, task.cacheLevel, e);
                }
            }, delay, TimeUnit.MILLISECONDS);

            pendingCleanupTasks.put(task.table + ":" + task.cacheLevel, future);
            submittedTasks.incrementAndGet();

            // 增加下一次任务的延迟时间，每个任务间隔1000毫秒
            delay += 1000;
        }
    }

    private void doCleanCacheDependencyByFreshness(Set<String> sortedMembers, Integer cacheLevel) {
        try {
            if (!sortedMembers.isEmpty()) {
                // 批量删除缓存
                stringRedisTemplate.delete(sortedMembers);
            }
        } catch (Exception e) {
            log.error("删除缓存依赖关系时发生错误: sortedMembers={}, level={}", sortedMembers, cacheLevel, e);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }

    /**
     * 缓存清理任务类，实现了Comparable接口以支持优先级排序
     */
    private record CacheCleanupTask(
            String table,
            long previousVersion,
            Set<String> sortedMembers,
            Integer cacheLevel) implements Comparable<CacheCleanupTask> {

        @Override
        public int compareTo(CacheCleanupTask other) {
            // 按照缓存级别排序，级别数字越小优先级越高
            return this.cacheLevel.compareTo(other.cacheLevel);
        }
    }
}