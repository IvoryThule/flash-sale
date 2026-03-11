package com.flashsale.controller;

import com.flashsale.common.Result;
import com.flashsale.entity.Order;
import com.flashsale.service.OrderService;
import com.flashsale.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 查询用户订单列表
     * GET /api/order/list
     */
    @GetMapping("/list")
    public Result<List<Order>> list() {
        Long userId = UserContext.getUserId();
        List<Order> orders = orderService.getUserOrders(userId);
        return Result.success(orders);
    }

    /**
     * 查询订单详情
     * GET /api/order/{id}
     */
    @GetMapping("/{id}")
    public Result<Order> detail(@PathVariable("id") Long id) {
        Order order = orderService.getOrderDetail(id);
        return Result.success(order);
    }

    /**
     * 取消订单
     * POST /api/order/cancel/{id}
     */
    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable("id") Long id) {
        Long userId = UserContext.getUserId();
        orderService.cancelOrder(id, userId);
        return Result.success();
    }

    /**
     * 支付订单（模拟）
     * POST /api/order/pay/{id}?payChannel=1
     */
    @PostMapping("/pay/{id}")
    public Result<Void> pay(@PathVariable("id") Long id,
                            @RequestParam("payChannel") int payChannel) {
        Long userId = UserContext.getUserId();
        orderService.payOrder(id, userId, payChannel);
        return Result.success();
    }
}
