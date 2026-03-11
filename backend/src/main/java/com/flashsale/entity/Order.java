package com.flashsale.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private Long eventId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Integer orderType;
    private Integer status;
    private Integer payChannel;
    private LocalDateTime payTime;
    private LocalDateTime expireTime;
    private Long addressId;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
