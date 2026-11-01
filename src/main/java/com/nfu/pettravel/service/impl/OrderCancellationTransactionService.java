package com.nfu.pettravel.service.impl;

import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.entity.AppointmentOrder;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.mapper.AppointmentOrderMapper;
import com.nfu.pettravel.mapper.CampRouteMapper;
import com.nfu.pettravel.service.CancelledOrder;
import com.nfu.pettravel.service.InventoryTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCancellationTransactionService {
    private final AppointmentOrderMapper orderMapper;
    private final CampRouteMapper routeMapper;
    private final InventoryTaskService inventoryTaskService;

    public OrderCancellationTransactionService(
            AppointmentOrderMapper orderMapper,
            CampRouteMapper routeMapper,
            InventoryTaskService inventoryTaskService) {
        this.orderMapper = orderMapper;
        this.routeMapper = routeMapper;
        this.inventoryTaskService = inventoryTaskService;
    }

    @Transactional(rollbackFor = Exception.class)
    public CancelledOrder cancelWaitPay(long orderId, Long expectedUserId) {
        AppointmentOrder order = orderMapper.selectById(orderId);
        if (order == null || Integer.valueOf(1).equals(order.getIsDeleted())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (orderMapper.cancelWaitPay(orderId, expectedUserId) != 1) {
            return CancelledOrder.alreadyHandled(orderId, order.getRouteId());
        }
        if (routeMapper.increaseStock(order.getRouteId(), 1) != 1) {
            throw new BusinessException(ErrorCode.INVENTORY_OPERATION_FAILED);
        }
        inventoryTaskService.createOrderReleaseTask(orderId, order.getRouteId());
        return CancelledOrder.cancelled(orderId, order.getRouteId());
    }
}
