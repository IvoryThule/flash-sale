package com.flashsale.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 秒杀异步下单消息体。
 */
@Data
public class SeckillOrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId;
    private Long userId;
    private Long eventId;
    private Long sendTimestamp;
}
