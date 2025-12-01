package top.noaharno.cachedependency.mapper;

import org.apache.ibatis.annotations.*;
import top.noaharno.cachedependency.entity.Product;

import java.util.List;

@Mapper
public interface ProductMapper {
    
    @Select("SELECT * FROM products WHERE id = #{id}")
    Product selectById(Long id);
    
    @Select("SELECT * FROM products")
    List<Product> selectAll();
    
    @Insert("INSERT INTO products(name, description, price, stock) VALUES(#{name}, #{description}, #{price}, #{stock})")
    @Options(keyProperty = "id", keyColumn = "id", useGeneratedKeys = true)
    int insert(Product product);
    
    @Update("UPDATE products SET name = #{name}, description = #{description}, price = #{price}, stock = #{stock} WHERE id = #{id}")
    int update(Product product);
    
    @Delete("DELETE FROM products WHERE id = #{id}")
    int deleteById(Long id);
}