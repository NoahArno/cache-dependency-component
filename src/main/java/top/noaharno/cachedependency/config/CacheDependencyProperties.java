package top.noaharno.cachedependency.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * 缓存一致性配置属性类
 * 用于接收application.yml中关于缓存一致性的配置项
 *
 * @author NoahArno
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "cache.dependency")
public class CacheDependencyProperties {

    /**
     * 是否启用缓存一致性功能
     */
    private boolean enabled = true;

    /**
     * 版本key的前缀，默认为"cache:version:"
     */
    private String versionKeyPrefix = "cache:version:";

    /**
     * 依赖关系key的前缀，默认为"cache:dependency:"
     */
    private String dependencyKeyPrefix = "cache:dependency:";

    /**
     * 涉及缓存的表
     */
    private Set<String> tables = new HashSet<>();

    /**
     * 异步清理缓存的线程池大小
     */
    private int cleanThreadPoolSize = 10;
}