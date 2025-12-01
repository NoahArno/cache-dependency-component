package top.noaharno.cacheconsistency.integration;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import top.noaharno.cacheconsistency.TestApplication;
import top.noaharno.cacheconsistency.config.CacheConsistencyProperties;
import top.noaharno.cacheconsistency.constant.CacheLevelEnum;
import top.noaharno.cacheconsistency.interceptor.TableAnalysisInterceptor;
import top.noaharno.cacheconsistency.service.CacheDependencyService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("test")
public class CacheConsistencyIntegrationTest {


    @Autowired
    private TableAnalysisInterceptor tableAnalysisInterceptor;

    @Autowired
    private CacheDependencyService cacheDependencyService;

    @Autowired
    private CacheConsistencyProperties cacheConsistencyProperties;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
        // 测试Spring上下文是否能正常加载
        assertTrue(true);
    }

    @Test
    void testBeansAreCreated() {
        // 测试所需的Bean是否被正确创建
        assertNotNull(tableAnalysisInterceptor);
        assertNotNull(cacheDependencyService);
        assertNotNull(cacheConsistencyProperties);
    }

    @Test
    void testCacheDependencyServiceFunctionality() {
        // 测试缓存依赖服务的基本功能
        String versionKey = cacheDependencyService.getVersionKey("users");
        String dependencyKey = cacheDependencyService.getDependencyKey("users", "1");

        assertNotNull(versionKey);
        assertNotNull(dependencyKey);
        assertTrue(versionKey.contains("users"));
        assertTrue(dependencyKey.contains("users") && dependencyKey.contains("1"));
    }

    @Test
    void testCacheLevelEnumValues() {
        // 测试缓存级别枚举值
        assertTrue(CacheLevelEnum.SECONDS.getLevel() < CacheLevelEnum.MINUTES.getLevel());
        assertTrue(CacheLevelEnum.MINUTES.getLevel() < CacheLevelEnum.HOURS.getLevel());
        assertTrue(CacheLevelEnum.HOURS.getLevel() < CacheLevelEnum.DAYS.getLevel());
    }
}