package top.noaharno.cachedependency.integration;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import top.noaharno.cachedependency.TestApplication;
import top.noaharno.cachedependency.config.CacheDependencyProperties;
import top.noaharno.cachedependency.constant.CacheLevelEnum;
import top.noaharno.cachedependency.entity.User;
import top.noaharno.cachedependency.interceptor.TableAnalysisInterceptor;
import top.noaharno.cachedependency.mapper.UserMapper;
import top.noaharno.cachedependency.mapper.OrderMapper;
import top.noaharno.cachedependency.mapper.ProductMapper;
import top.noaharno.cachedependency.service.CacheDependencyService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("test")
public class CacheDependencySimpleTest {


    @Autowired
    private TableAnalysisInterceptor tableAnalysisInterceptor;

    @Autowired
    private CacheDependencyService cacheDependencyService;

    @Autowired
    private CacheDependencyProperties cacheDependencyProperties;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private ProductMapper productMapper;

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
        assertNotNull(cacheDependencyProperties);
        assertNotNull(userMapper);
        assertNotNull(orderMapper);
        assertNotNull(productMapper);
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
    
    @Test
    void testDatabaseConnectionAndMyBatisIntegration() {
        // 测试数据库连接和MyBatis集成
        // 查询所有用户
        var users = userMapper.selectAll();
        assertFalse(users.isEmpty(), "应该能够查询到用户数据");
        assertEquals(3, users.size(), "应该有3个用户");
        
        // 根据ID查询用户
        var user = userMapper.selectById(1L);
        assertNotNull(user, "应该能够根据ID查询到用户");
        assertEquals("Alice", user.getName(), "用户名应该匹配");
        
        // 插入新用户
        User newUser = new User();
        newUser.setName("David");
        newUser.setEmail("david@example.com");
        newUser.setAge(35);
        int insertResult = userMapper.insert(newUser);
        assertEquals(1, insertResult, "应该成功插入新用户");
        assertNotNull(newUser.getId(), "新用户的ID应该被生成");
        
        // 更新用户
        newUser.setAge(36);
        int updateResult = userMapper.update(newUser);
        assertEquals(1, updateResult, "应该成功更新用户");
        
        // 删除用户
        int deleteResult = userMapper.deleteById(newUser.getId());
        assertEquals(1, deleteResult, "应该成功删除用户");
    }
    
    @Test
    void testOrderAndProductOperations() {
        // 测试订单和产品操作
        
        // 查询所有产品
        var products = productMapper.selectAll();
        assertFalse(products.isEmpty(), "应该能够查询到产品数据");
        assertEquals(3, products.size(), "应该有3个产品");
        
        // 查询所有订单
        var orders = orderMapper.selectAll();
        assertFalse(orders.isEmpty(), "应该能够查询到订单数据");
        assertEquals(3, orders.size(), "应该有3个订单");
        
        // 根据用户ID查询订单
        var userOrders = orderMapper.selectByUserId(1L);
        assertFalse(userOrders.isEmpty(), "应该能够根据用户ID查询到订单");
        assertEquals(1, userOrders.size(), "用户应该有一个订单");
        
        // 插入新产品
        var newProduct = new top.noaharno.cachedependency.entity.Product();
        newProduct.setName("Monitor");
        newProduct.setDescription("27-inch 4K Monitor");
        newProduct.setPrice(300.00);
        newProduct.setStock(25);
        int insertResult = productMapper.insert(newProduct);
        assertEquals(1, insertResult, "应该成功插入新产品");
        assertNotNull(newProduct.getId(), "新产品的ID应该被生成");
        
        // 插入新订单
        var newOrder = new top.noaharno.cachedependency.entity.Order();
        newOrder.setUserId(1L);
        newOrder.setProductName("Monitor");
        newOrder.setPrice(300.00);
        newOrder.setQuantity(1);
        int orderInsertResult = orderMapper.insert(newOrder);
        assertEquals(1, orderInsertResult, "应该成功插入新订单");
        assertNotNull(newOrder.getId(), "新订单的ID应该被生成");
    }
}