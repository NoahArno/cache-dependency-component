package top.noaharno.cachedependency;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * @author NoahArno
 * @version 1.0.0
 * @since 2025/12/1 14:26
 */
@EnableCaching
@SpringBootApplication
@MapperScan(basePackages = "top.noaharno.cachedependency.mapper")
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}