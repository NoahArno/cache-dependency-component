package top.noaharno.cacheconsistency.interceptor;

import top.noaharno.cacheconsistency.config.CacheConsistencyProperties;
import top.noaharno.cacheconsistency.constant.CacheLevelEnum;
import top.noaharno.cacheconsistency.service.CacheDependencyService;
import top.noaharno.cacheconsistency.util.SqlAnalysisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.javassist.bytecode.analysis.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


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

    private final StringRedisTemplate redisTemplate;

    private final CacheDependencyService cacheDependencyService;

    public TableAnalysisInterceptor(StringRedisTemplate redisTemplate,
                                    CacheDependencyService cacheDependencyService) {
        this.redisTemplate = redisTemplate;
        this.cacheDependencyService = cacheDependencyService;
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
            // 清空缓存依赖关系
            for (String table : tableSet) {
                CompletableFuture.runAsync(() -> {
                    // 清空之前，自增版本号
                    long incrementedVersion = redisTemplate.opsForValue().increment(cacheDependencyService.getVersionKey(table));
                    if (incrementedVersion == 1) {
                        // 版本号为 1 的时候，表示一开始缓存里面没有任何依赖关系，不需要进行任何处理
                        return;
                    }
                    Long previousVersion = incrementedVersion - 1;
                    // 获取 ZSET 中的所有成员，按照缓存新鲜度级别分别进行清除
                    for (CacheLevelEnum cacheLevel : CacheLevelEnum.getSortedValues()) {
                        doCleanCacheDependencyByFreshness(table, previousVersion, cacheLevel.getLevel());
                    }
                });
            }
        } catch (Exception e) {
            // 报错不抛出，不阻断正常业务执行
            log.warn("清空缓存依赖关系失败：{}，所涉及的表为：{}", e.getMessage(), tableSet, e);
        }
    }

    private void doCleanCacheDependencyByFreshness(String table, Long previousVersion, Integer cacheLevel) {
        // 获取到当前缓存级别的缓存依赖关系
        Set<String> sortedMembers = redisTemplate.opsForZSet().rangeByScore(cacheDependencyService.getDependencyKey(table, String.valueOf(previousVersion)), cacheLevel, cacheLevel);
        redisTemplate.delete(sortedMembers);
        // FIXME 休眠 5 秒，避免同时清除大量 Key 导致业务操作阻塞，以及缓存雪崩
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
            if (log.isDebugEnabled()) {
                log.debug("线程 Sleep 失败，被中断");
            }
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
}
