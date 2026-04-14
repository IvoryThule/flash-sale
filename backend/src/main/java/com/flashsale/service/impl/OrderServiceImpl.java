package com.flashsale.service.impl;

import com.flashsale.common.BusinessException;
import com.flashsale.common.Constants;
import com.flashsale.common.ResultCode;
import com.flashsale.entity.Order;
import com.flashsale.entity.Product;
import com.flashsale.entity.SeckillEvent;
import com.flashsale.entity.SeckillOrder;
import com.flashsale.mapper.OrderMapper;
import com.flashsale.mapper.ProductMapper;
import com.flashsale.mapper.SeckillEventMapper;
import com.flashsale.mapper.SeckillOrderMapper;
import com.flashsale.mapper.StockMapper;
import com.flashsale.datasource.Master;
import com.flashsale.service.OrderService;
import com.flashsale.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
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
    private final SeckillEventMapper seckillEventMapper;
    private final ProductMapper productMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final StockMapper stockMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Value("${flash-sale.seckill.order-expire-minutes:15}")
    private int orderExpireMinutes;

    @Override
    @Master
    @Transactional(rollbackFor = Exception.class)
    public Order createSeckillOrder(Long userId, Long eventId) {
        SeckillEvent event = seckillEventMapper.selectById(eventId);
        if (event == null) {
            throw new BusinessException(ResultCode.SECKILL_EVENT_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        if (event.getStartTime() != null && now.isBefore(event.getStartTime())) {
            throw new BusinessException(ResultCode.SECKILL_NOT_START);
        }
        if (event.getEndTime() != null && now.isAfter(event.getEndTime())) {
            throw new BusinessException(ResultCode.SECKILL_ENDED);
        }

        SeckillOrder existed = seckillOrderMapper.selectByUserIdAndEventId(userId, eventId);
        if (existed != null) {
            throw new BusinessException(ResultCode.SECKILL_REPEATED);
        }

        // 基于 stock.version 的乐观锁扣减，防止超卖。
        var stock = stockMapper.selectByEventId(eventId);
        if (stock == null || stock.getAvailable() == null || stock.getAvailable() <= 0) {
            throw new BusinessException(ResultCode.SECKILL_SOLD_OUT);
        }
        int affected = stockMapper.deductStock(eventId, 1, stock.getVersion());
        if (affected == 0) {
            throw new BusinessException(ResultCode.SECKILL_SOLD_OUT);
        }

        Product product = productMapper.selectById(event.getProductId());
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        Order order = new Order();
        order.setOrderNo(String.valueOf(snowflakeIdGenerator.nextId()));
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setEventId(eventId);
        order.setProductName(product.getName());
        order.setProductImage(product.getImage());
        order.setQuantity(1);
        order.setUnitPrice(event.getSeckillPrice());
        order.setTotalPrice(event.getSeckillPrice());
        order.setOrderType(Constants.ORDER_TYPE_SECKILL);
        order.setStatus(Constants.ORDER_STATUS_UNPAID);
        order.setExpireTime(now.plusMinutes(orderExpireMinutes));
        orderMapper.insert(order);

        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setUserId(userId);
        seckillOrder.setProductId(product.getId());
        seckillOrder.setEventId(eventId);
        seckillOrder.setOrderId(order.getId());
        seckillOrder.setStatus(1);

        try {
            seckillOrderMapper.insert(seckillOrder);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.SECKILL_REPEATED);
        }

        log.info("秒杀订单创建成功: userId={}, eventId={}, orderId={}", userId, eventId, order.getId());
        return order;
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
