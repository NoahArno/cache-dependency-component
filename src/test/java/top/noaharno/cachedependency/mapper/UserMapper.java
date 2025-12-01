package top.noaharno.cachedependency.mapper;

import org.apache.ibatis.annotations.*;
import top.noaharno.cachedependency.entity.User;

import java.util.List;

@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    User selectById(Long id);
    
    @Select("SELECT * FROM users")
    List<User> selectAll();
    
    @Insert("INSERT INTO users(name, email, age) VALUES(#{name}, #{email}, #{age})")
    @Options(keyProperty = "id", keyColumn = "id", useGeneratedKeys = true)
    int insert(User user);
    
    @Update("UPDATE users SET name = #{name}, email = #{email}, age = #{age} WHERE id = #{id}")
    int update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(Long id);
}