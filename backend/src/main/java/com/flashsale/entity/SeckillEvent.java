package com.flashsale.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动实体
 */
@Data
public class SeckillEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private Long productId;
    private BigDecimal seckillPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalStock;
    private Integer availableStock;
    private Integer limitPerUser;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
