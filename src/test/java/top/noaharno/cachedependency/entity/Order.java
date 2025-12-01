package top.noaharno.cachedependency.entity;

import lombok.Data;

@Data
public class Order {
    private Long id;
    private Long userId;
    private String productName;
    private Double price;
    private Integer quantity;
}