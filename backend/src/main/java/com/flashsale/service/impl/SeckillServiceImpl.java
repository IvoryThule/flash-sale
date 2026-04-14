package com.flashsale.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.BusinessException;
import com.flashsale.common.Constants;
import com.flashsale.common.ResultCode;
import com.flashsale.dto.SeckillOrderMessage;
import com.flashsale.entity.Order;
import com.flashsale.entity.SeckillEvent;
import com.flashsale.mapper.SeckillEventMapper;
import com.flashsale.mapper.SeckillOrderMapper;
import com.flashsale.service.OrderService;
import com.flashsale.service.SeckillService;
import com.flashsale.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀服务实现（骨架，核心逻辑在 Step 7 中完善）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillEventMapper seckillEventMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final OrderService orderService;
    private final StringRedisTemplate stringRedisTemplate;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${flash-sale.seckill.async-enabled:false}")
    private boolean asyncEnabled;

    @Value("${flash-sale.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${flash-sale.kafka.seckill-topic:flash-sale-seckill-order}")
    private String seckillTopic;

    @Override
    public Long executeSeckill(Long userId, Long eventId) {
        SeckillEvent event = validateEventAndTime(eventId);
        LocalDateTime now = LocalDateTime.now();

        String userSeckillKey = Constants.REDIS_SECKILL_USER + userId + ":" + eventId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(userSeckillKey))) {
            throw new BusinessException(ResultCode.SECKILL_REPEATED);
        }

        String soldOutKey = Constants.REDIS_SECKILL_OVER + eventId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(soldOutKey))) {
            throw new BusinessException(ResultCode.SECKILL_SOLD_OUT);
        }

        String stockKey = Constants.REDIS_SECKILL_STOCK + eventId;
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
            preheatStock(eventId);
        }

        String userLockKey = Constants.REDIS_LOCK_SECKILL + eventId + ":user:" + userId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(userLockKey, "1", 3, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ResultCode.SECKILL_BUSY);
        }

        Long remain = null;
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(userSeckillKey))) {
                throw new BusinessException(ResultCode.SECKILL_REPEATED);
            }

            remain = stringRedisTemplate.opsForValue().decrement(stockKey);
            if (remain == null) {
                throw new BusinessException(ResultCode.SECKILL_BUSY);
            }
            if (remain < 0) {
                stringRedisTemplate.opsForValue().increment(stockKey);
                stringRedisTemplate.opsForValue().set(soldOutKey, "1", 1, TimeUnit.HOURS);
                throw new BusinessException(ResultCode.SECKILL_SOLD_OUT);
            }

            Order order = orderService.createSeckillOrder(userId, eventId);
            long ttlSeconds = 86400;
            if (event.getEndTime() != null && event.getEndTime().isAfter(now)) {
                ttlSeconds = Math.max(Duration.between(now, event.getEndTime()).getSeconds(), ttlSeconds);
            }
            stringRedisTemplate.opsForValue().set(userSeckillKey, "1", ttlSeconds, TimeUnit.SECONDS);

            if (remain == 0) {
                stringRedisTemplate.opsForValue().set(soldOutKey, "1", 1, TimeUnit.HOURS);
            }
            return order.getId();
        } catch (BusinessException ex) {
            // 创建订单失败时回补 Redis 预扣库存。
            if (remain != null && remain >= 0) {
                stringRedisTemplate.opsForValue().increment(stockKey);
            }
            throw ex;
        } catch (Exception ex) {
            if (remain != null && remain >= 0) {
                stringRedisTemplate.opsForValue().increment(stockKey);
            }
            throw new BusinessException(ResultCode.SECKILL_BUSY);
        } finally {
            stringRedisTemplate.delete(userLockKey);
        }
    }

    @Override
    public String executeSeckillAsync(Long userId, Long eventId) {
        if (!asyncEnabled || !kafkaEnabled) {
            throw new BusinessException(ResultCode.SECKILL_BUSY.getCode(), "异步秒杀功能未开启");
        }

        SeckillEvent event = validateEventAndTime(eventId);
        LocalDateTime now = LocalDateTime.now();

        String userSeckillKey = Constants.REDIS_SECKILL_USER + userId + ":" + eventId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(userSeckillKey))) {
            throw new BusinessException(ResultCode.SECKILL_REPEATED);
        }

        String soldOutKey = Constants.REDIS_SECKILL_OVER + eventId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(soldOutKey))) {
            throw new BusinessException(ResultCode.SECKILL_SOLD_OUT);
        }

        String stockKey = Constants.REDIS_SECKILL_STOCK + eventId;
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
            preheatStock(eventId);
        }

        String userLockKey = Constants.REDIS_LOCK_SECKILL + eventId + ":user:" + userId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(userLockKey, "1", 3, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ResultCode.SECKILL_BUSY);
        }

        Long remain = null;
        String requestId = String.valueOf(snowflakeIdGenerator.nextId());
        String resultKey = Constants.REDIS_SECKILL_RESULT + requestId;
        String reqMapKey = Constants.REDIS_SECKILL_REQ_MAP + requestId;
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(userSeckillKey))) {
                throw new BusinessException(ResultCode.SECKILL_REPEATED);
            }

            remain = stringRedisTemplate.opsForValue().decrement(stockKey);
            if (remain == null) {
                throw new BusinessException(ResultCode.SECKILL_BUSY);
            }
            if (remain < 0) {
                stringRedisTemplate.opsForValue().increment(stockKey);
                stringRedisTemplate.opsForValue().set(soldOutKey, "1", 1, TimeUnit.HOURS);
                throw new BusinessException(ResultCode.SECKILL_SOLD_OUT);
            }

            long ttlSeconds = 86400;
            if (event.getEndTime() != null && event.getEndTime().isAfter(now)) {
                ttlSeconds = Math.max(Duration.between(now, event.getEndTime()).getSeconds(), ttlSeconds);
            }
            stringRedisTemplate.opsForValue().set(userSeckillKey, "PENDING", ttlSeconds, TimeUnit.SECONDS);
            stringRedisTemplate.opsForValue().set(resultKey, "PROCESSING", 1, TimeUnit.DAYS);
            stringRedisTemplate.opsForValue().set(reqMapKey, userId + ":" + eventId, 1, TimeUnit.DAYS);

            SeckillOrderMessage msg = new SeckillOrderMessage();
            msg.setRequestId(requestId);
            msg.setUserId(userId);
            msg.setEventId(eventId);
            msg.setSendTimestamp(System.currentTimeMillis());
            kafkaTemplate.send(seckillTopic, String.valueOf(eventId), toJson(msg));

            if (remain == 0) {
                stringRedisTemplate.opsForValue().set(soldOutKey, "1", 1, TimeUnit.HOURS);
            }
            return requestId;
        } catch (BusinessException ex) {
            if (remain != null && remain >= 0) {
                stringRedisTemplate.opsForValue().increment(stockKey);
            }
            stringRedisTemplate.delete(resultKey);
            stringRedisTemplate.delete(reqMapKey);
            stringRedisTemplate.delete(userSeckillKey);
            throw ex;
        } catch (Exception ex) {
            if (remain != null && remain >= 0) {
                stringRedisTemplate.opsForValue().increment(stockKey);
            }
            stringRedisTemplate.delete(resultKey);
            stringRedisTemplate.delete(reqMapKey);
            stringRedisTemplate.delete(userSeckillKey);
            throw new BusinessException(ResultCode.SECKILL_BUSY);
        } finally {
            stringRedisTemplate.delete(userLockKey);
        }
    }

    @Override
    public Map<String, Object> queryAsyncResult(Long userId, String requestId) {
        String reqMap = stringRedisTemplate.opsForValue().get(Constants.REDIS_SECKILL_REQ_MAP + requestId);
        if (reqMap == null || reqMap.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "请求不存在或已过期");
        }

        String[] arr = reqMap.split(":");
        if (arr.length != 2) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "请求数据损坏");
        }

        Long ownerUserId = Long.parseLong(arr[0]);
        Long eventId = Long.parseLong(arr[1]);
        if (!ownerUserId.equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        String result = stringRedisTemplate.opsForValue().get(Constants.REDIS_SECKILL_RESULT + requestId);
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("eventId", eventId);

        if (result == null || result.isEmpty() || "PROCESSING".equals(result)) {
            var seckillOrder = seckillOrderMapper.selectByUserIdAndEventId(userId, eventId);
            if (seckillOrder != null) {
                data.put("status", "SUCCESS");
                data.put("orderId", seckillOrder.getOrderId());
            } else {
                data.put("status", "PROCESSING");
            }
            return data;
        }

        if (result.startsWith("SUCCESS:")) {
            data.put("status", "SUCCESS");
            data.put("orderId", Long.parseLong(result.substring("SUCCESS:".length())));
            return data;
        }

        if (result.startsWith("FAILED:")) {
            data.put("status", "FAILED");
            data.put("reason", result.substring("FAILED:".length()));
            return data;
        }

        data.put("status", "UNKNOWN");
        data.put("raw", result);
        return data;
    }

    @Override
    public List<SeckillEvent> getOngoingEvents() {
        return seckillEventMapper.selectOngoingEvents();
    }

    @Override
    public SeckillEvent getEventDetail(Long eventId) {
        SeckillEvent event = seckillEventMapper.selectById(eventId);
        if (event == null) {
            throw new BusinessException(ResultCode.SECKILL_EVENT_NOT_FOUND);
        }
        return event;
    }

    @Override
    public void preheatStock(Long eventId) {
        SeckillEvent event = seckillEventMapper.selectById(eventId);
        if (event == null) {
            throw new BusinessException(ResultCode.SECKILL_EVENT_NOT_FOUND);
        }
        int available = event.getAvailableStock() == null ? 0 : Math.max(event.getAvailableStock(), 0);
        String stockKey = Constants.REDIS_SECKILL_STOCK + eventId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(available), 2, TimeUnit.HOURS);

        String soldOutKey = Constants.REDIS_SECKILL_OVER + eventId;
        if (available <= 0) {
            stringRedisTemplate.opsForValue().set(soldOutKey, "1", 2, TimeUnit.HOURS);
        } else {
            stringRedisTemplate.delete(soldOutKey);
        }
        log.info("秒杀库存预热完成: eventId={}, available={}", eventId, available);
    }

    private SeckillEvent validateEventAndTime(Long eventId) {
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
        return event;
    }

    private String toJson(SeckillOrderMessage msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SECKILL_BUSY);
        }
    }
}
