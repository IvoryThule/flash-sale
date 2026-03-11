package com.flashsale.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存实体
 */
@Data
public class Stock implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long productId;
    private Long eventId;
    private Integer total;
    private Integer available;
    private Integer locked;
    private Integer sold;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
