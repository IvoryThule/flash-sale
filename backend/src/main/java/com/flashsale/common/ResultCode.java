package com.flashsale.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ==================== 通用 ====================
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),

    // ==================== 用户模块 ====================
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户名已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    PHONE_ALREADY_EXISTS(1004, "手机号已被注册"),
    USER_DISABLED(1005, "用户已被禁用"),

    // ==================== 商品模块 ====================
    PRODUCT_NOT_FOUND(2001, "商品不存在"),
    PRODUCT_OFF_SHELF(2002, "商品已下架"),

    // ==================== 秒杀模块 ====================
    SECKILL_NOT_START(3001, "秒杀活动尚未开始"),
    SECKILL_ENDED(3002, "秒杀活动已结束"),
    SECKILL_SOLD_OUT(3003, "已售罄"),
    SECKILL_REPEATED(3004, "请勿重复秒杀"),
    SECKILL_LIMIT_EXCEEDED(3005, "超过限购数量"),
    SECKILL_BUSY(3006, "系统繁忙，请稍后重试"),
    SECKILL_EVENT_NOT_FOUND(3007, "秒杀活动不存在"),

    // ==================== 订单模块 ====================
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_STATUS_ERROR(4002, "订单状态异常"),
    ORDER_EXPIRED(4003, "订单已超时"),

    // ==================== 库存模块 ====================
    STOCK_NOT_ENOUGH(5001, "库存不足"),
    STOCK_DEDUCT_FAIL(5002, "库存扣减失败");

    /** 状态码 */
    private final int code;

    /** 状态消息 */
    private final String message;
}
