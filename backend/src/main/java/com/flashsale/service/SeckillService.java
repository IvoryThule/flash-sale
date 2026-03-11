package com.flashsale.service;

import com.flashsale.entity.SeckillEvent;

import java.util.List;

/**
 * 秒杀服务接口
 */
public interface SeckillService {

    /**
     * 执行秒杀
     *
     * @param userId  用户ID
     * @param eventId 秒杀活动ID
     * @return 订单ID
     */
    Long executeSeckill(Long userId, Long eventId);

    /**
     * 查询进行中的秒杀活动
     */
    List<SeckillEvent> getOngoingEvents();

    /**
     * 查询秒杀活动详情
     */
    SeckillEvent getEventDetail(Long eventId);

    /**
     * 预热秒杀库存到 Redis
     */
    void preheatStock(Long eventId);
}
