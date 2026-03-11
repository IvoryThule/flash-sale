package com.flashsale.mapper;

import com.flashsale.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 库存 Mapper
 */
@Mapper
public interface StockMapper {

    /**
     * 根据活动ID查询库存
     */
    Stock selectByEventId(@Param("eventId") Long eventId);

    /**
     * 乐观锁扣减库存（核心防超卖）
     * UPDATE stock SET available = available - #{quantity},
     *   locked = locked + #{quantity}, version = version + 1
     * WHERE event_id = #{eventId} AND version = #{version} AND available >= #{quantity}
     */
    int deductStock(@Param("eventId") Long eventId,
                    @Param("quantity") int quantity,
                    @Param("version") int version);

    /**
     * 确认出库（锁定转已售）
     */
    int confirmSold(@Param("eventId") Long eventId, @Param("quantity") int quantity);

    /**
     * 释放锁定库存（超时取消订单）
     */
    int releaseLocked(@Param("eventId") Long eventId, @Param("quantity") int quantity);

    /**
     * 新增库存记录
     */
    int insert(Stock stock);
}
