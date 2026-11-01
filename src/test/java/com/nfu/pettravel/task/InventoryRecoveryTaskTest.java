package com.nfu.pettravel.task;

import com.nfu.pettravel.entity.AppointmentOrder;
import com.nfu.pettravel.entity.CampRoute;
import com.nfu.pettravel.entity.InventoryCompensationTask;
import com.nfu.pettravel.enums.InventoryTaskStatus;
import com.nfu.pettravel.enums.InventoryTaskType;
import com.nfu.pettravel.inventory.InventoryGateway;
import com.nfu.pettravel.mapper.AppointmentOrderMapper;
import com.nfu.pettravel.mapper.CampRouteMapper;
import com.nfu.pettravel.mapper.InventoryCompensationTaskMapper;
import com.nfu.pettravel.service.InventoryReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryRecoveryTaskTest {
    private InventoryCompensationTaskMapper taskMapper;
    private AppointmentOrderMapper orderMapper;
    private CampRouteMapper routeMapper;
    private InventoryGateway inventoryGateway;
    private InventoryReconciliationService reconciliationService;
    private InventoryRecoveryTask recoveryTask;

    @BeforeEach
    void setUp() {
        taskMapper = mock(InventoryCompensationTaskMapper.class);
        orderMapper = mock(AppointmentOrderMapper.class);
        routeMapper = mock(CampRouteMapper.class);
        inventoryGateway = mock(InventoryGateway.class);
        reconciliationService = mock(InventoryReconciliationService.class);
        recoveryTask = new InventoryRecoveryTask(
                taskMapper, orderMapper, routeMapper, inventoryGateway, reconciliationService,
                100, 10, Duration.ofSeconds(30));
    }

    @Test
    void shouldProcessOnlyTaskWonByDatabaseCas() {
        InventoryCompensationTask task = task(1L, 11L, 8L, InventoryTaskType.ORDER_RELEASE);
        when(taskMapper.findDueTasks(any(), eq(100))).thenReturn(List.of(task));
        when(taskMapper.claim(1L)).thenReturn(0);

        recoveryTask.retryDurableTasks();

        verify(inventoryGateway, never()).releaseOrder(8L, 11L, 1);
    }

    @Test
    void shouldRetryIdempotentRedisOperationAndMarkSuccess() {
        InventoryCompensationTask task = task(1L, 11L, 8L, InventoryTaskType.RESERVATION_CONFIRM);
        when(taskMapper.findDueTasks(any(), eq(100))).thenReturn(List.of(task));
        when(taskMapper.claim(1L)).thenReturn(1);

        recoveryTask.retryDurableTasks();

        verify(inventoryGateway).confirm(8L, 11L);
        verify(taskMapper).complete(1L);
    }

    @Test
    void shouldCompensateExpiredReservationOnlyWhenMysqlOrderDoesNotExist() {
        CampRoute route = new CampRoute();
        route.setId(8L);
        when(routeMapper.selectList(any())).thenReturn(List.of(route));
        when(inventoryGateway.findExpiredReservations(eq(8L), any(Long.class), eq(100)))
                .thenReturn(Set.of(11L, 12L));
        AppointmentOrder committed = new AppointmentOrder();
        committed.setId(12L);
        when(orderMapper.selectById(12L)).thenReturn(committed);

        recoveryTask.recoverExpiredReservations();

        verify(inventoryGateway).compensateReservation(8L, 11L);
        verify(inventoryGateway).confirm(8L, 12L);
    }

    @Test
    void shouldNotReconcileAcrossUnfinishedReleaseOrCompensationTask() {
        CampRoute route = new CampRoute();
        route.setId(8L);
        route.setAvailableStock(5);
        when(routeMapper.selectList(any())).thenReturn(List.of(route));
        when(reconciliationService.reconcileAndMarkRequested(8L)).thenReturn(false);

        recoveryTask.reconcileRedisWithMysql();

        verify(reconciliationService).reconcileAndMarkRequested(8L);
    }

    @Test
    void shouldSafelyReconcileTasksEscalatedAfterRetryExhaustion() {
        when(taskMapper.findNeedsReconcileRouteIds(100)).thenReturn(List.of(8L));
        when(reconciliationService.reconcile(8L)).thenReturn(true);

        recoveryTask.retryDurableTasks();

        verify(taskMapper).markNeedsReconcileSucceeded(8L);
    }

    private InventoryCompensationTask task(
            long id, long orderId, long routeId, InventoryTaskType type) {
        InventoryCompensationTask task = new InventoryCompensationTask();
        task.setId(id);
        task.setOrderId(orderId);
        task.setRouteId(routeId);
        task.setTaskType(type);
        task.setTaskStatus(InventoryTaskStatus.PENDING);
        task.setRetryCount(0);
        task.setNextRetryTime(LocalDateTime.now());
        return task;
    }
}
