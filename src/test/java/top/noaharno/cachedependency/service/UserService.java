package top.noaharno.cachedependency.service;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import top.noaharno.cachedependency.constant.CacheLevelEnum;
import top.noaharno.cachedependency.entity.User;
import top.noaharno.cachedependency.mapper.UserMapper;

/**
 * @author NoahArno
 * @version 1.0.0
 * @since 2025/12/1 15:48
 */
@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private CacheDependencyService cacheDependencyService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public User getUserById(Long id) {
        String string = stringRedisTemplate.opsForValue().get("user:" + id);
        if (StringUtils.isBlank(string)) {
            User user = userMapper.selectById(id);
            stringRedisTemplate.opsForValue().set("user:" + id, JSON.toJSONString(user));
            cacheDependencyService.recordDependencies("user:" + id, CacheLevelEnum.SECONDS.getLevel(), "users");
            return user;
        } else {
            return JSON.parseObject(string, User.class);
        }
    }
}
