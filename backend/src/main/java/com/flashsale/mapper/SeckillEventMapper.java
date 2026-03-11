package com.flashsale.mapper;

import com.flashsale.entity.SeckillEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 秒杀活动 Mapper
 */
@Mapper
public interface SeckillEventMapper {

    /**
     * 根据ID查询秒杀活动
     */
    SeckillEvent selectById(@Param("id") Long id);

    /**
     * 查询进行中的秒杀活动列表
     */
    List<SeckillEvent> selectOngoingEvents();

    /**
     * 查询商品关联的秒杀活动
     */
    List<SeckillEvent> selectByProductId(@Param("productId") Long productId);

    /**
     * 扣减活动库存（乐观锁）
     */
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * 回补活动库存
     */
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * 更新活动状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") int status);
}
