package com.flashsale.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.BusinessException;
import com.flashsale.common.Constants;
import com.flashsale.dto.SeckillOrderMessage;
import com.flashsale.entity.Order;
import com.flashsale.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Kafka 秒杀下单消费者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "flash-sale.kafka.enabled", havingValue = "true")
public class SeckillOrderConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final StringRedisTemplate stringRedisTemplate;

    @KafkaListener(topics = "${flash-sale.kafka.seckill-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        SeckillOrderMessage msg;
        try {
            msg = objectMapper.readValue(payload, SeckillOrderMessage.class);
        } catch (Exception e) {
            log.error("Kafka消息解析失败: payload={}", payload, e);
            return;
        }

        String requestId = msg.getRequestId();
        Long userId = msg.getUserId();
        Long eventId = msg.getEventId();

        String doneKey = Constants.REDIS_SECKILL_MSG_DONE + requestId;
        Boolean firstConsume = stringRedisTemplate.opsForValue().setIfAbsent(doneKey, "1", 1, TimeUnit.DAYS);
        if (!Boolean.TRUE.equals(firstConsume)) {
            log.debug("重复消息跳过: requestId={}", requestId);
            return;
        }

        String resultKey = Constants.REDIS_SECKILL_RESULT + requestId;
        String userKey = Constants.REDIS_SECKILL_USER + userId + ":" + eventId;
        String stockKey = Constants.REDIS_SECKILL_STOCK + eventId;
        String soldOutKey = Constants.REDIS_SECKILL_OVER + eventId;

        try {
            Order order = orderService.createSeckillOrder(userId, eventId);
            stringRedisTemplate.opsForValue().set(resultKey, "SUCCESS:" + order.getId(), 1, TimeUnit.DAYS);
            stringRedisTemplate.opsForValue().set(userKey, "1", 1, TimeUnit.DAYS);
            log.info("异步秒杀下单成功: requestId={}, orderId={}", requestId, order.getId());
        } catch (BusinessException e) {
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.delete(soldOutKey);
            stringRedisTemplate.delete(userKey);
            stringRedisTemplate.opsForValue().set(resultKey, "FAILED:" + e.getCode() + ":" + e.getMessage(), 1, TimeUnit.DAYS);
            log.warn("异步秒杀业务失败: requestId={}, code={}, message={}", requestId, e.getCode(), e.getMessage());
        } catch (Exception e) {
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.delete(soldOutKey);
            stringRedisTemplate.delete(userKey);
            stringRedisTemplate.opsForValue().set(resultKey, "FAILED:3006:系统繁忙，请稍后重试", 1, TimeUnit.DAYS);
            log.error("异步秒杀系统异常: requestId={}", requestId, e);
        }
    }
}
