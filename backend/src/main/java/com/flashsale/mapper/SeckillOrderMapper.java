package com.flashsale.mapper;

import com.flashsale.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 秒杀订单 Mapper
 */
@Mapper
public interface SeckillOrderMapper {

    /**
     * 新增秒杀订单
     */
    int insert(SeckillOrder seckillOrder);

    /**
     * 查询用户在某活动中的秒杀订单（判断是否已秒杀）
     */
    SeckillOrder selectByUserIdAndEventId(@Param("userId") Long userId,
                                          @Param("eventId") Long eventId);

    /**
     * 根据订单ID查询秒杀订单
     */
    SeckillOrder selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 更新秒杀订单状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") int status);
}
