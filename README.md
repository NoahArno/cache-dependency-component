# Cache Consistency Spring Boot Starter

缓存依赖 Spring Boot Starter 组件，用于解决分布式系统中的缓存依赖问题。

## 功能特性

- 基于 Redis ZSet 实现缓存与数据表的依赖关系管理
- 自动版本控制，确保缓存数据的新鲜度
- 支持新鲜度权重配置，优先清理高优先级缓存
- 异步清理机制，不影响主业务流程

## 设计原理

1. 通过 Redis 中的 ZSET 维护缓存和数据表的依赖关系，ZSET 中的 key 为具体的表名，value 为具体的业务名称，SCORE 为当前业务的新鲜度（新鲜度越高的，SCORE 值越小，在清除缓存时优先）
2. 基于 Mybatis 的 Plugin 拦截所有 update 请求，并解析出该语句所涉及的表名。当数据表更新的时候，就能知道当前数据表被哪些业务依赖了
3. 为每个表都维护一个版本号，比如 ZSET 中的 Key 就为"表名:version"。当数据表更新的时候，将版本号+1，然后将旧的 Key 中的 Value 列表（具体的业务）进行批量删除，优先删除新鲜度要求高的业务。此时如果有新的数据需要被缓存，就会先读取对应表的最新版本号，然后往 ZSET 中添加新的业务依赖关系

## 使用方法

### 1. 添加依赖

```xml
<dependency>
    <groupId>top.noaharno</groupId>
    <artifactId>cache-dependency-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置文件

```yaml
# 缓存依赖性配置
cache:
  dependency:
    enabled: true # 是否启用缓存依赖性功能
    version-key-prefix: 'test:version:' # 版本号key的前缀
    dependency-key-prefix: 'test:dependency:' # 缓存依赖关系key的前缀
    clean-thread-pool-size: 5 # 异步清理缓存的线程池大小
    # 缓存表：如果表被缓存依赖，就需要在这里配置，用于减少访问 Redis 的压力，具体见 TableAnalysisInterceptor#cleanCacheDependency
    tables:
      - users
      - orders
      - products
# 配置 Redis 缓存
spring:
  redis:
    host: localhost
    port: 6379
```

### 3. 使用方法

在业务代码放入缓存的时候，将本次缓存数据所涉及到的表名和新鲜度作为参数，交给本组件维护
```java
@Service
public class MenuService {
    
    @CacheConsistency(businessName = "menu", tables = {"t_user", "t_menu"}, freshness = 5)
    public List<Menu> getMenuList() {
        // 业务逻辑
        return menuList;
    }
}
```

### 4. 表更新时触发缓存清理

```java
@Service
public class UserService {

    private final UserMapper userMapper;
    private final CacheDependencyService cacheDependencyService;
    private final StringRedisTemplate stringRedisTemplate;
    
    public UserService(UserMapper userMapper, CacheDependencyService cacheDependencyService, StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.cacheDependencyService = cacheDependencyService;
        this.stringRedisTemplate = stringRedisTemplate;
    }
    
    public User getUserById(Long id) {
        String string = stringRedisTemplate.opsForValue().get("user:" + id);
        // 如果 Redis 中不存在，就读取数据库
        if (StringUtils.isBlank(string)) {
            User user = userMapper.selectById(id);
            // 将缓存值存储到 Redis 中
            stringRedisTemplate.opsForValue().set("user:" + id, JSON.toJSONString(user));
            // 重点：记录缓存依赖关系，其中参数是缓存的key，缓存的新鲜度，表名
            cacheDependencyService.recordDependencies("user:" + id, CacheLevelEnum.SECONDS.getLevel(), "users");
            return user;
        } else {
            return JSON.parseObject(string, User.class);
        }
    }
}
```

## 配置项说明

| 配置项                                      | 默认值                 | 说明 |
|------------------------------------------|---------------------| --- |
| cache.dependency.enabled                 | true                | 是否启用缓存一致性功能 |
| cache.dependency.version-key-prefix     | "cache:version:"    | 版本key的前缀 |
| cache.dependency.dependency-key-prefix  | "cache:dependency:" | 依赖关系key的前缀 |
| cache.dependency.clean-thread-pool-size | 10                  | 异步清理缓存的线程池大小 |
| cache.dependency.tables       | 空集合                 | 缓存表：如果表被缓存依赖，就需要在这里配置，用于减少访问 Redis 的压力，具体见 TableAnalysisInterceptor#cleanCacheDependency |

## 核心组件

- `TableAnalysisInterceptor`: MyBatis 拦截器，负责拦截所有 update 语句，解析出该语句所涉及的表名，并删除旧的依赖关系
- `CacheDependencyService`: 缓存依赖关系服务，负责管理缓存依赖关系

## 工作流程

1. 业务方法执行时，通过 CacheDependencyService#recordDependencies 建立缓存依赖关系
2. 当数据表更新时，组件底层通过 TableAnalysisInterceptor 拦截 update 语句，处理所涉及到的缓存的清理

## 注意事项

1. 需要配置Redis连接信息
2. 需依赖 Mybatis Plugin
3. 业务方法需要是Spring管理的Bean