package top.noaharno.cachedependency.mapper;

import org.apache.ibatis.annotations.*;
import top.noaharno.cachedependency.entity.Order;

import java.util.List;

@Mapper
public interface OrderMapper {
    
    @Select("SELECT * FROM orders WHERE id = #{id}")
    Order selectById(Long id);
    
    @Select("SELECT * FROM orders WHERE user_id = #{userId}")
    List<Order> selectByUserId(Long userId);
    
    @Select("SELECT * FROM orders")
    List<Order> selectAll();
    
    @Insert("INSERT INTO orders(user_id, product_name, price, quantity) VALUES(#{userId}, #{productName}, #{price}, #{quantity})")
    @Options(keyProperty = "id", keyColumn = "id", useGeneratedKeys = true)
    int insert(Order order);
    
    @Update("UPDATE orders SET user_id = #{userId}, product_name = #{productName}, price = #{price}, quantity = #{quantity} WHERE id = #{id}")
    int update(Order order);
    
    @Delete("DELETE FROM orders WHERE id = #{id}")
    int deleteById(Long id);
}