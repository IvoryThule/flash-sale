package com.flashsale.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private String image;
    private BigDecimal price;
    private String category;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
