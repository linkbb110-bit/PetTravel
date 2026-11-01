package com.nfu.pettravel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.pettravel.entity.InventoryCompensationTask;
import com.nfu.pettravel.enums.InventoryTaskStatus;
import com.nfu.pettravel.enums.InventoryTaskType;
import com.nfu.pettravel.mapper.InventoryCompensationTaskMapper;
import com.nfu.pettravel.service.InventoryTaskService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class InventoryTaskServiceImpl implements InventoryTaskService {
    private final InventoryCompensationTaskMapper taskMapper;

    public InventoryTaskServiceImpl(InventoryCompensationTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Override
    public void createReservationConfirmTask(long orderId, long routeId) {
        insertTask(orderId, routeId, InventoryTaskType.RESERVATION_CONFIRM, null);
    }

    @Override
    public void markReservationConfirmed(long orderId) {
        taskMapper.markSucceeded(orderId, InventoryTaskType.RESERVATION_CONFIRM.getValue());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordBookingRollback(long orderId, long routeId, String reason) {
        insertTask(orderId, routeId, InventoryTaskType.BOOKING_ROLLBACK, reason);
    }

    @Override
    public void createOrderReleaseTask(long orderId, long routeId) {
        insertTask(orderId, routeId, InventoryTaskType.ORDER_RELEASE, null);
    }

    @Override
    public void markOrderReleased(long orderId) {
        taskMapper.markSucceeded(orderId, InventoryTaskType.ORDER_RELEASE.getValue());
    }

    @Override
    public void requestRouteReconciliation(long routeId) {
        LocalDateTime now = LocalDateTime.now();
        if (taskMapper.resetRouteReconciliation(routeId, now) == 1) {
            return;
        }
        try {
            insertTask(routeId, routeId, InventoryTaskType.ROUTE_RECONCILE, null);
        } catch (DuplicateKeyException concurrentInsert) {
            taskMapper.resetRouteReconciliation(routeId, now);
        }
    }

    private void insertTask(long orderId, long routeId, InventoryTaskType taskType, String reason) {
        InventoryCompensationTask existing = taskMapper.selectOne(
                new LambdaQueryWrapper<InventoryCompensationTask>()
                        .eq(InventoryCompensationTask::getOrderId, orderId)
                        .eq(InventoryCompensationTask::getTaskType, taskType));
        if (existing != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        InventoryCompensationTask task = new InventoryCompensationTask();
        task.setOrderId(orderId);
        task.setRouteId(routeId);
        task.setTaskType(taskType);
        task.setTaskStatus(InventoryTaskStatus.PENDING);
        task.setRetryCount(0);
        task.setNextRetryTime(now);
        task.setLastError(reason);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        try {
            taskMapper.insert(task);
        } catch (DuplicateKeyException ignored) {
            // The unique (order_id, task_type) key makes concurrent inserts idempotent.
        }
    }
}
