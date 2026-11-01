package com.nfu.pettravel.service;

import com.nfu.pettravel.inventory.InventoryGateway;
import com.nfu.pettravel.mapper.CampRouteMapper;
import com.nfu.pettravel.mapper.InventoryCompensationTaskMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryReconciliationServiceTest {

    @Test
    void shouldAcquireRedisLockBeforeReadingMysqlAndAlwaysUnlock() {
        InventoryGateway gateway = mock(InventoryGateway.class);
        CampRouteMapper routeMapper = mock(CampRouteMapper.class);
        InventoryCompensationTaskMapper taskMapper = mock(InventoryCompensationTaskMapper.class);
        InventoryReconciliationService service =
                new InventoryReconciliationService(gateway, routeMapper, taskMapper);

        when(gateway.tryAcquireReconciliationLock(8L)).thenReturn("token");
        when(routeMapper.selectAvailableStockForUpdate(8L)).thenReturn(7);
        when(taskMapper.countBlockingByRoute(8L)).thenReturn(0L);
        when(gateway.synchronizeStockUnderLock(8L, "token", 7)).thenReturn(true);

        assertThat(service.reconcile(8L)).isTrue();

        InOrder order = inOrder(gateway, routeMapper, taskMapper);
        order.verify(gateway).tryAcquireReconciliationLock(8L);
        order.verify(routeMapper).selectAvailableStockForUpdate(8L);
        order.verify(taskMapper).countBlockingByRoute(8L);
        order.verify(gateway).synchronizeStockUnderLock(8L, "token", 7);
        order.verify(gateway).releaseReconciliationLock(8L, "token");
    }

    @Test
    void shouldNotOverwriteRedisWhileInventoryTasksAreUnfinished() {
        InventoryGateway gateway = mock(InventoryGateway.class);
        CampRouteMapper routeMapper = mock(CampRouteMapper.class);
        InventoryCompensationTaskMapper taskMapper = mock(InventoryCompensationTaskMapper.class);
        InventoryReconciliationService service =
                new InventoryReconciliationService(gateway, routeMapper, taskMapper);

        when(gateway.tryAcquireReconciliationLock(8L)).thenReturn("token");
        when(routeMapper.selectAvailableStockForUpdate(8L)).thenReturn(7);
        when(taskMapper.countBlockingByRoute(8L)).thenReturn(1L);

        assertThat(service.reconcile(8L)).isFalse();
        verify(gateway).releaseReconciliationLock(8L, "token");
    }

    @Test
    void shouldReleaseRedisLockWhenMysqlReadFails() {
        InventoryGateway gateway = mock(InventoryGateway.class);
        CampRouteMapper routeMapper = mock(CampRouteMapper.class);
        InventoryCompensationTaskMapper taskMapper = mock(InventoryCompensationTaskMapper.class);
        InventoryReconciliationService service =
                new InventoryReconciliationService(gateway, routeMapper, taskMapper);

        when(gateway.tryAcquireReconciliationLock(8L)).thenReturn("token");
        when(routeMapper.selectAvailableStockForUpdate(8L)).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.reconcile(8L))
                .isInstanceOf(IllegalStateException.class);
        verify(gateway).releaseReconciliationLock(8L, "token");
    }

    @Test
    void shouldMarkRequestedRouteTaskInTheSameReconciliationOperation() {
        InventoryGateway gateway = mock(InventoryGateway.class);
        CampRouteMapper routeMapper = mock(CampRouteMapper.class);
        InventoryCompensationTaskMapper taskMapper = mock(InventoryCompensationTaskMapper.class);
        InventoryReconciliationService service =
                new InventoryReconciliationService(gateway, routeMapper, taskMapper);
        when(gateway.tryAcquireReconciliationLock(8L)).thenReturn("token");
        when(routeMapper.selectAvailableStockForUpdate(8L)).thenReturn(7);
        when(taskMapper.countBlockingByRoute(8L)).thenReturn(0L);
        when(gateway.synchronizeStockUnderLock(8L, "token", 7)).thenReturn(true);

        assertThat(service.reconcileAndMarkRequested(8L)).isTrue();

        verify(taskMapper).markSucceeded(8L, "ROUTE_RECONCILE");
    }
}
