package com.flashsale.mapper;

import com.flashsale.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper {

    /**
     * 新增订单
     */
    int insert(Order order);

    /**
     * 根据ID查询订单
     */
    Order selectById(@Param("id") Long id);

    /**
     * 根据订单号查询订单
     */
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询用户的订单列表
     */
    List<Order> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询用户指定状态的订单
     */
    List<Order> selectByUserIdAndStatus(@Param("userId") Long userId,
                                        @Param("status") int status);

    /**
     * 更新订单状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") int status);

    /**
     * 支付订单
     */
    int payOrder(@Param("id") Long id,
                 @Param("payChannel") int payChannel,
                 @Param("payTime") LocalDateTime payTime);

    /**
     * 查询超时未支付的订单
     */
    List<Order> selectExpiredOrders(@Param("now") LocalDateTime now);
}
