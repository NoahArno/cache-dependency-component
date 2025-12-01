package top.noaharno.cacheconsistency.config;

import top.noaharno.cacheconsistency.interceptor.TableAnalysisInterceptor;
import top.noaharno.cacheconsistency.service.CacheDependencyService;
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
@EnableConfigurationProperties(CacheConsistencyProperties.class)
public class CacheConsistencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate() {
        return new StringRedisTemplate();
    }

    @Bean
    public TableAnalysisInterceptor tableAnalysisInterceptor(StringRedisTemplate redisTemplate,
                                                             CacheDependencyService cacheDependencyService) {
        return new TableAnalysisInterceptor(redisTemplate, cacheDependencyService);
    }

    @Bean
    public CacheDependencyService cacheDependencyService(StringRedisTemplate redisTemplate, CacheConsistencyProperties cacheConsistencyProperties) {
        return new CacheDependencyService(redisTemplate, cacheConsistencyProperties);
    }
}