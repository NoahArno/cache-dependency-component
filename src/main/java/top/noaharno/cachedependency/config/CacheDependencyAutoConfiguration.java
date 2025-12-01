package top.noaharno.cachedependency.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.DependsOn;
import top.noaharno.cachedependency.interceptor.TableAnalysisInterceptor;
import top.noaharno.cachedependency.service.CacheDependencyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存一致性自动配置类
 * 该类负责自动配置缓存一致性组件所需的所有Bean
 *
 * @author NoahArno
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(CacheDependencyProperties.class)
@ConditionalOnBooleanProperty(prefix = "cache.dependency", value = "enabled", matchIfMissing = true)
public class CacheDependencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate() {
        return new StringRedisTemplate();
    }

    @Bean
    public TableAnalysisInterceptor tableAnalysisInterceptor(StringRedisTemplate stringRedisTemplate,
                                                             CacheDependencyService cacheDependencyService,
                                                             CacheDependencyProperties cacheDependencyProperties) {
        return new TableAnalysisInterceptor(stringRedisTemplate, cacheDependencyService, cacheDependencyProperties);
    }

    @Bean
    public CacheDependencyService cacheDependencyService(StringRedisTemplate stringRedisTemplate, CacheDependencyProperties cacheDependencyProperties) {
        return new CacheDependencyService(stringRedisTemplate, cacheDependencyProperties);
    }
}