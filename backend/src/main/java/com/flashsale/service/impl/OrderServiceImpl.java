package com.flashsale.service.impl;

import com.flashsale.common.BusinessException;
import com.flashsale.common.Constants;
import com.flashsale.common.ResultCode;
import com.flashsale.entity.Order;
import com.flashsale.entity.SeckillOrder;
import com.flashsale.mapper.OrderMapper;
import com.flashsale.mapper.SeckillOrderMapper;
import com.flashsale.mapper.StockMapper;
import com.flashsale.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final StockMapper stockMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Order createSeckillOrder(Long userId, Long eventId) {
        // 此方法在 SeckillService 中被调用，具体实现在 Step 7/8 完善
        throw new UnsupportedOperationException("将在 Step 7/8 中实现");
    }

    @Override
    public Order getOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    @Override
    public List<Order> getUserOrders(Long userId) {
        return orderMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (order.getStatus() != Constants.ORDER_STATUS_UNPAID) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 更新订单状态为已取消
        orderMapper.updateStatus(orderId, Constants.ORDER_STATUS_CANCELLED);

        // 如果是秒杀订单，释放库存
        if (order.getOrderType() == Constants.ORDER_TYPE_SECKILL && order.getEventId() != null) {
            stockMapper.releaseLocked(order.getEventId(), order.getQuantity());

            // Redis 库存回补
            stringRedisTemplate.opsForValue().increment(
                    Constants.REDIS_SECKILL_STOCK + order.getEventId());

            // 清除秒杀结束标记（如果有库存了）
            stringRedisTemplate.delete(Constants.REDIS_SECKILL_OVER + order.getEventId());

            log.info("订单取消，库存已释放: orderId={}, eventId={}", orderId, order.getEventId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, Long userId, int payChannel) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (order.getStatus() != Constants.ORDER_STATUS_UNPAID) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        // 检查是否超时
        if (LocalDateTime.now().isAfter(order.getExpireTime())) {
            throw new BusinessException(ResultCode.ORDER_EXPIRED);
        }

        // 更新订单为已支付
        orderMapper.payOrder(orderId, payChannel, LocalDateTime.now());

        // 如果是秒杀订单，确认出库（锁定 → 已售）
        if (order.getOrderType() == Constants.ORDER_TYPE_SECKILL && order.getEventId() != null) {
            stockMapper.confirmSold(order.getEventId(), order.getQuantity());
        }

        log.info("订单支付成功: orderId={}, payChannel={}", orderId, payChannel);
    }
}
