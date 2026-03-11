package com.flashsale.service.impl;

import com.flashsale.common.BusinessException;
import com.flashsale.common.ResultCode;
import com.flashsale.entity.SeckillEvent;
import com.flashsale.mapper.SeckillEventMapper;
import com.flashsale.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 秒杀服务实现（骨架，核心逻辑在 Step 7 中完善）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillEventMapper seckillEventMapper;

    @Override
    public Long executeSeckill(Long userId, Long eventId) {
        // 将在 Step 7 中实现完整秒杀流程
        throw new UnsupportedOperationException("将在 Step 7 中实现");
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
        // 将在 Step 9 中实现 Redis 缓存预热
        throw new UnsupportedOperationException("将在 Step 9 中实现");
    }
}
