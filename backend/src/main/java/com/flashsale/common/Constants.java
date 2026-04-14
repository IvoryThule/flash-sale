package com.flashsale.common;

/**
 * 常量定义
 */
public final class Constants {

    private Constants() {}

    // ==================== Redis Key 前缀 ====================

    /** 秒杀活动库存 Key: seckill:stock:{eventId} */
    public static final String REDIS_SECKILL_STOCK = "seckill:stock:";

    /** 秒杀活动商品详情 Key: seckill:event:{eventId} */
    public static final String REDIS_SECKILL_EVENT = "seckill:event:";

    /** 商品详情 Key: product:detail:{productId} */
    public static final String REDIS_PRODUCT_DETAIL = "product:detail:";

    /** 分布式锁 Key: lock:seckill:{eventId} */
    public static final String REDIS_LOCK_SECKILL = "lock:seckill:";

    /** 用户秒杀记录 Key: seckill:user:{userId}:{eventId} */
    public static final String REDIS_SECKILL_USER = "seckill:user:";

    /** 秒杀结束标记 Key: seckill:over:{eventId} */
    public static final String REDIS_SECKILL_OVER = "seckill:over:";

    /** 异步秒杀请求结果 Key: seckill:result:{requestId} */
    public static final String REDIS_SECKILL_RESULT = "seckill:result:";

    /** 异步秒杀请求映射 Key: seckill:reqmap:{requestId}，value=userId:eventId */
    public static final String REDIS_SECKILL_REQ_MAP = "seckill:reqmap:";

    /** 异步消息消费幂等 Key: seckill:msg:done:{requestId} */
    public static final String REDIS_SECKILL_MSG_DONE = "seckill:msg:done:";

    /** JWT Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** JWT Header 名称 */
    public static final String TOKEN_HEADER = "Authorization";

    // ==================== 订单类型 ====================

    public static final int ORDER_TYPE_NORMAL = 1;
    public static final int ORDER_TYPE_SECKILL = 2;

    // ==================== 订单状态 ====================

    public static final int ORDER_STATUS_UNPAID = 0;
    public static final int ORDER_STATUS_PAID = 1;
    public static final int ORDER_STATUS_CANCELLED = 2;
    public static final int ORDER_STATUS_REFUNDED = 3;
    public static final int ORDER_STATUS_EXPIRED = 4;

    // ==================== 秒杀活动状态 ====================

    public static final int EVENT_STATUS_NOT_STARTED = 0;
    public static final int EVENT_STATUS_ONGOING = 1;
    public static final int EVENT_STATUS_ENDED = 2;
    public static final int EVENT_STATUS_CANCELLED = 3;
}
