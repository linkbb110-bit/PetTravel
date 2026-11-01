package com.nfu.pettravel.mapper;

import com.nfu.pettravel.entity.AppointmentOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
* @author 11707
* @description 针对表【appointment_order(携宠游订单记录表)】的数据库操作Mapper
* @createDate 2026-05-07 08:13:11
* @Entity com.nfu.pettravel.entity.AppointmentOrder
*/
public interface AppointmentOrderMapper extends BaseMapper<AppointmentOrder> {
    long countActiveBooking(@Param("userId") long userId, @Param("routeId") long routeId);

    int cancelWaitPay(@Param("orderId") long orderId, @Param("userId") Long userId);

    int markPaid(@Param("orderId") long orderId, @Param("userId") long userId);

    int markCompleted(@Param("orderId") long orderId);

    List<Long> findTimeoutOrderIds(@Param("deadline") LocalDateTime deadline, @Param("limit") int limit);

}




