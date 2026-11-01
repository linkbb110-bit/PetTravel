package com.nfu.pettravel.service;

import com.nfu.pettravel.cache.RouteCacheService;
import com.nfu.pettravel.dto.RouteUpsertRequest;
import com.nfu.pettravel.entity.CampRoute;
import com.nfu.pettravel.mapper.CampRouteMapper;
import com.nfu.pettravel.service.impl.CampRouteServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampRouteServiceImplTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldKeepCommittedRouteSuccessfulWhenPostCommitRedisCallFails() {
        CampRouteMapper routeMapper = mock(CampRouteMapper.class);
        RouteCacheService cache = mock(RouteCacheService.class);
        BookingOrchestrator booking = mock(BookingOrchestrator.class);
        InventoryReconciliationService reconciliation = mock(InventoryReconciliationService.class);
        InventoryTaskService tasks = mock(InventoryTaskService.class);
        doAnswer(invocation -> {
            CampRoute route = invocation.getArgument(0);
            route.setId(8L);
            return 1;
        }).when(routeMapper).insert(any(CampRoute.class));
        when(reconciliation.reconcileAndMarkRequested(8L))
                .thenThrow(new IllegalStateException("redis down"));
        CampRouteServiceImpl service =
                new CampRouteServiceImpl(routeMapper, cache, booking, reconciliation, tasks);
        RouteUpsertRequest request = new RouteUpsertRequest(
                "route", BigDecimal.TEN, 10, 1);
        TransactionSynchronizationManager.initSynchronization();

        long routeId = service.createRoute(request);
        List<TransactionSynchronization> callbacks =
                TransactionSynchronizationManager.getSynchronizations();
        TransactionSynchronizationManager.clearSynchronization();

        assertThat(routeId).isEqualTo(8L);
        assertThatCode(() -> callbacks.forEach(TransactionSynchronization::afterCommit))
                .doesNotThrowAnyException();
        verify(tasks).requestRouteReconciliation(8L);
    }
}
