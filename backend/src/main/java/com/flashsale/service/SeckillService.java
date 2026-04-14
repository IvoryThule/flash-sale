package com.flashsale.service;

import com.flashsale.entity.SeckillEvent;

import java.util.List;
import java.util.Map;

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
     * 异步执行秒杀（Kafka）
     *
     * @return 请求ID（用于轮询结果）
     */
    String executeSeckillAsync(Long userId, Long eventId);

    /**
     * 查询异步秒杀结果
     */
    Map<String, Object> queryAsyncResult(Long userId, String requestId);

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
