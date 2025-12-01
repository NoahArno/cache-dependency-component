package top.noaharno.cacheconsistency.service;

import lombok.extern.slf4j.Slf4j;
import top.noaharno.cacheconsistency.config.CacheConsistencyProperties;
import io.micrometer.common.util.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存依赖服务实现类
 * <p>
 * 通过Redis的ZSet数据结构实现缓存与数据表之间的依赖关系管理
 *
 * @author NoahArno
 * @since 1.0.0
 */
@Slf4j
public class CacheDependencyService {

    private static final String INIT_VERSION = "1";

    private final StringRedisTemplate redisTemplate;

    private final CacheConsistencyProperties properties;

    public CacheDependencyService(StringRedisTemplate redisTemplate, CacheConsistencyProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 添加缓存依赖关系
     *
     * @param businessKey       业务缓存键
     * @param freshness 当前业务的新鲜度权重
     * @param tables    当前业务依赖的表名
     */
    public void recordDependencies(String businessKey, int freshness, String... tables) {
        for (String tableName : tables) {
            if (!properties.getTables().contains(tableName)) {
                // 如果表名不在配置的表中，需要进行报错，避免表更新时缓存被遗漏
                throw new RuntimeException("Table " + tableName + " is not in the configured tables.");
            }
            recordDependency(tableName, businessKey, freshness);
        }
    }

    private void recordDependency(String tableName, String businessKey, int freshness) {
        // 1. 获取当前 table 最新的版本号
        String versionKey = getVersionKey(tableName);
        String currentVersion = redisTemplate.opsForValue().get(versionKey);
        if (StringUtils.isBlank(currentVersion)) {
            // 2. 如果 table 没有版本号，则设置初始版本号为 1
            redisTemplate.opsForValue().set(versionKey, INIT_VERSION);
            currentVersion = INIT_VERSION;
        }
        redisTemplate.opsForZSet().add(getDependencyKey(tableName, currentVersion), businessKey, freshness);
    }

    /**
     * 获取表版本号键
     * @param tableName 表名
     * @return 表版本号键
     */
    public String getVersionKey(String tableName) {
        return properties.getVersionKeyPrefix() + tableName;
    }

    /**
     * 获取表依赖关系键
     * @param tableName 表名
     * @param version 版本号
     * @return 表依赖关系键
     */
    public String getDependencyKey(String tableName, String version) {
        return properties.getDependencyKeyPrefix() + tableName + ":" + version;
    }


}