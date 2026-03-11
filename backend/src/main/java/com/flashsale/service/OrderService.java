package com.flashsale.service;

import com.flashsale.entity.Order;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建秒杀订单
     */
    Order createSeckillOrder(Long userId, Long eventId);

    /**
     * 查询订单详情
     */
    Order getOrderDetail(Long orderId);

    /**
     * 查询用户订单列表
     */
    List<Order> getUserOrders(Long userId);

    /**
     * 取消订单
     */
    void cancelOrder(Long orderId, Long userId);

    /**
     * 支付订单（模拟）
     */
    void payOrder(Long orderId, Long userId, int payChannel);
}
