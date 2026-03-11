package com.flashsale.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秒杀订单实体
 */
@Data
public class SeckillOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long productId;
    private Long eventId;
    private Long orderId;
    private Integer status;
    private LocalDateTime createdAt;
}
