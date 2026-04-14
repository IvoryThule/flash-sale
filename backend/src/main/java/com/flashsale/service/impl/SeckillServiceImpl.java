package com.flashsale.service.impl;

import com.flashsale.common.BusinessException;
import com.flashsale.common.Constants;
import com.flashsale.common.ResultCode;
import com.flashsale.entity.Order;
import com.flashsale.entity.SeckillEvent;
import com.flashsale.mapper.SeckillEventMapper;
import com.flashsale.service.OrderService;
import com.flashsale.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀服务实现（骨架，核心逻辑在 Step 7 中完善）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillEventMapper seckillEventMapper;
    private final OrderService orderService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Long executeSeckill(Long userId, Long eventId) {
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
}
