package top.noaharno.cachedependency.integration;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import top.noaharno.cachedependency.TestApplication;
import top.noaharno.cachedependency.entity.User;
import top.noaharno.cachedependency.mapper.UserMapper;
import top.noaharno.cachedependency.service.CacheDependencyService;
import top.noaharno.cachedependency.service.UserService;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("test")
public class CacheDependencyFunctionalTest {

    @Autowired
    private CacheDependencyService cacheDependencyService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @BeforeEach
    void setup() {
        flushDb();
    }

    public void flushDb() {
        // 清空缓存
        stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
            connection.serverCommands().flushDb();
            return true;
        });
    }

    @Test
    void testCacheConsistencyMechanism() throws InterruptedException {
        // 执行数据库获取操作
        String key = "user:1";
        User user = userService.getUserById(1L);
        assertNotNull(user, "此时能够查询到用户");
        User user1 = userService.getUserById(1L);
        assertEquals(user, user1, "两次查询结果应该一致");
        String string = stringRedisTemplate.opsForValue().get(key);
        User user2 = JSON.parseObject(string, User.class);
        assertEquals(user, user2, "两次查询结果应该一致，因为是从缓存里面查询的");
        String dependencyKeyPattern = cacheDependencyService.getDependencyKey("users", "1");
        Set<String> keys = stringRedisTemplate.keys(dependencyKeyPattern);
        assertEquals(1, keys.size(), "此时存在依赖键");
        // 此时更新用户，但是没有显示进行 Redis 的删除
        user.setAge(user.getAge() + 1);
        int updateResult = userMapper.update(user);
        assertEquals(1, updateResult, "应该成功更新用户");
        // 等待一段时间让异步清理任务执行
        Thread.sleep(2000);
        // 此时缓存里面应该值不存在
        assertTrue(StringUtils.isBlank(stringRedisTemplate.opsForValue().get(key)), "缓存应该被删除");
        assertEquals("2", stringRedisTemplate.opsForValue().get(cacheDependencyService.getVersionKey("users")), "版本号为1");
        // 检查相关的依赖键是否已被清理
        dependencyKeyPattern = cacheDependencyService.getDependencyKey("users", "1");
        keys = stringRedisTemplate.keys(dependencyKeyPattern);
        assertEquals(0, keys.size(), "依赖键应该被清理");

    }
    
    @Test
    void testMultipleTableOperations() throws InterruptedException {

        // 获取各表的初始版本号
        String userVersionKey = cacheDependencyService.getVersionKey("users");
        String orderVersionKey = cacheDependencyService.getVersionKey("orders");
        String productVersionKey = cacheDependencyService.getVersionKey("products");
        
        // 对三个表分别进行更新操作
        User user = new User();
        user.setName("TestUser");
        user.setEmail("test@example.com");
        user.setAge(25);
        userMapper.insert(user);
        
        // 等待异步处理
        Thread.sleep(2000);
        
        // 验证各表版本号的变化
        String updatedUserVersion = stringRedisTemplate.opsForValue().get(userVersionKey);
        String updatedOrderVersion = stringRedisTemplate.opsForValue().get(orderVersionKey);
        String updatedProductVersion = stringRedisTemplate.opsForValue().get(productVersionKey);
        
        assertNotNull(updatedUserVersion, "用户表版本号应该存在");
        assertEquals(updatedUserVersion, "1", "当Redis为空，然后执行数据库update操作时，对应的表版本号应该被初始化为1");
        
        // 其他表的版本号应该保持不变（因为我们只更新了用户表）
        assertNull(updatedOrderVersion, "订单表版本号应该不存在");
        assertNull(updatedProductVersion, "产品表版本号应该不存在");
    }
}