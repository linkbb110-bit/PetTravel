package com.nfu.pettravel.service;

import com.nfu.pettravel.entity.AppointmentOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
* @author 11707
* @description 针对表【appointment_order(携宠游订单记录表)】的数据库操作Service
* @createDate 2026-05-07 08:13:11
*/
public interface AppointmentOrderService extends IService<AppointmentOrder> {
    boolean pay(long orderId, long userId);

    boolean complete(long orderId);

    void exportTimeoutOrders(HttpServletResponse response) throws IOException;

}
