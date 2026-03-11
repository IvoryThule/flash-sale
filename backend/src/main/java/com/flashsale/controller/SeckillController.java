package com.flashsale.controller;

import com.flashsale.common.Result;
import com.flashsale.entity.SeckillEvent;
import com.flashsale.service.SeckillService;
import com.flashsale.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 秒杀控制器
 */
@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 执行秒杀
     * POST /api/seckill/{eventId}
     */
    @PostMapping("/{eventId}")
    public Result<Long> doSeckill(@PathVariable("eventId") Long eventId) {
        Long userId = UserContext.getUserId();
        Long orderId = seckillService.executeSeckill(userId, eventId);
        return Result.success("秒杀成功", orderId);
    }

    /**
     * 查询进行中的秒杀活动
     * GET /api/seckill/events
     */
    @GetMapping("/events")
    public Result<List<SeckillEvent>> ongoingEvents() {
        List<SeckillEvent> events = seckillService.getOngoingEvents();
        return Result.success(events);
    }

    /**
     * 查询秒杀活动详情
     * GET /api/seckill/event/{eventId}
     */
    @GetMapping("/event/{eventId}")
    public Result<SeckillEvent> eventDetail(@PathVariable("eventId") Long eventId) {
        SeckillEvent event = seckillService.getEventDetail(eventId);
        return Result.success(event);
    }
}
