package com.nfu.pettravel.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nfu.pettravel.entity.AppointmentOrder;
import com.nfu.pettravel.entity.CampRoute;
import com.nfu.pettravel.entity.InventoryCompensationTask;
import com.nfu.pettravel.enums.InventoryTaskType;
import com.nfu.pettravel.inventory.InventoryGateway;
import com.nfu.pettravel.mapper.AppointmentOrderMapper;
import com.nfu.pettravel.mapper.CampRouteMapper;
import com.nfu.pettravel.mapper.InventoryCompensationTaskMapper;
import com.nfu.pettravel.service.InventoryReconciliationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class InventoryRecoveryTask {
    private final InventoryCompensationTaskMapper taskMapper;
    private final AppointmentOrderMapper orderMapper;
    private final CampRouteMapper routeMapper;
    private final InventoryGateway inventoryGateway;
    private final InventoryReconciliationService reconciliationService;
    private final int batchSize;
    private final int maxRetries;
    private final Duration retryBaseDelay;

    public InventoryRecoveryTask(
            InventoryCompensationTaskMapper taskMapper,
            AppointmentOrderMapper orderMapper,
            CampRouteMapper routeMapper,
            InventoryGateway inventoryGateway,
            InventoryReconciliationService reconciliationService,
            @Value("${pettravel.inventory.recovery-batch-size:200}") int batchSize,
            @Value("${pettravel.inventory.max-retries:10}") int maxRetries,
            @Value("${pettravel.inventory.retry-base-delay:30s}") Duration retryBaseDelay) {
        this.taskMapper = taskMapper;
        this.orderMapper = orderMapper;
        this.routeMapper = routeMapper;
        this.inventoryGateway = inventoryGateway;
        this.reconciliationService = reconciliationService;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.retryBaseDelay = retryBaseDelay;
    }

    @Scheduled(
            fixedDelayString = "${pettravel.inventory.task-retry-delay:10000}",
            initialDelayString = "${pettravel.inventory.recovery-initial-delay:30000}")
    public void retryDurableTasks() {
        taskMapper.requeueStale(LocalDateTime.now().minusMinutes(2));
        List<InventoryCompensationTask> tasks = taskMapper.findDueTasks(LocalDateTime.now(), batchSize);
        for (InventoryCompensationTask task : tasks) {
            if (taskMapper.claim(task.getId()) != 1) {
                continue;
            }
            try {
                execute(task);
                taskMapper.complete(task.getId());
            } catch (RuntimeException failure) {
                int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
                long multiplier = 1L << Math.min(retryCount, 10);
                LocalDateTime next = LocalDateTime.now().plus(retryBaseDelay.multipliedBy(multiplier));
                taskMapper.retry(task.getId(), next, summarize(failure), maxRetries);
                log.warn("Inventory recovery deferred: taskId={}, type={}",
                        task.getId(), task.getTaskType(), failure);
            }
        }
        reconcileEscalatedTasks();
    }

    @Scheduled(
            fixedDelayString = "${pettravel.inventory.pending-scan-delay:30000}",
            initialDelayString = "${pettravel.inventory.recovery-initial-delay:30000}")
    public void recoverExpiredReservations() {
        long now = System.currentTimeMillis();
        for (CampRoute route : inventoryRoutes()) {
            Set<Long> orderIds = inventoryGateway.findExpiredReservations(route.getId(), now, batchSize);
            for (Long orderId : orderIds) {
                try {
                    AppointmentOrder order = orderMapper.selectById(orderId);
                    if (order == null) {
                        inventoryGateway.compensateReservation(route.getId(), orderId);
                    } else {
                        inventoryGateway.confirm(route.getId(), orderId);
                        taskMapper.markSucceeded(orderId, InventoryTaskType.RESERVATION_CONFIRM.getValue());
                    }
                } catch (RuntimeException failure) {
                    log.warn("Expired reservation recovery deferred: routeId={}, orderId={}",
                            route.getId(), orderId, failure);
                }
            }
        }
    }

    @Scheduled(cron = "${pettravel.inventory.reconcile-cron:0 0/10 * * * ?}")
    public void reconcileRedisWithMysql() {
        for (CampRoute route : inventoryRoutes()) {
            try {
                reconciliationService.reconcileAndMarkRequested(route.getId());
            } catch (RuntimeException failure) {
                log.warn("Inventory reconciliation deferred: routeId={}", route.getId(), failure);
            }
        }
    }

    private void execute(InventoryCompensationTask task) {
        switch (task.getTaskType()) {
            case RESERVATION_CONFIRM -> inventoryGateway.confirm(task.getRouteId(), task.getOrderId());
            case BOOKING_ROLLBACK -> inventoryGateway.compensateReservation(task.getRouteId(), task.getOrderId());
            case ORDER_RELEASE -> inventoryGateway.releaseOrder(task.getRouteId(), task.getOrderId(), 1);
            case ROUTE_RECONCILE -> {
                if (!reconciliationService.reconcile(task.getRouteId())) {
                    throw new IllegalStateException("Route inventory is busy");
                }
            }
        }
    }

    private void reconcileEscalatedTasks() {
        List<Long> routeIds = taskMapper.findNeedsReconcileRouteIds(batchSize);
        if (routeIds == null) {
            return;
        }
        for (Long routeId : routeIds) {
            try {
                if (reconciliationService.reconcile(routeId)) {
                    taskMapper.markNeedsReconcileSucceeded(routeId);
                }
            } catch (RuntimeException failure) {
                log.warn("Escalated inventory reconciliation deferred: routeId={}", routeId, failure);
            }
        }
    }

    private List<CampRoute> inventoryRoutes() {
        return routeMapper.selectList(Wrappers.<CampRoute>lambdaQuery()
                .eq(CampRoute::getIsDeleted, 0));
    }

    private String summarize(Throwable failure) {
        String message = failure.getMessage();
        String value = message == null ? failure.getClass().getSimpleName() : message;
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
