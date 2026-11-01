package com.nfu.pettravel.service;

import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.entity.AppointmentOrder;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.mapper.AppointmentOrderMapper;
import com.nfu.pettravel.mapper.CampRouteMapper;
import com.nfu.pettravel.service.impl.OrderCancellationTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderCancellationTransactionServiceTest {
    private AppointmentOrderMapper orderMapper;
    private CampRouteMapper routeMapper;
    private InventoryTaskService inventoryTaskService;
    private OrderCancellationTransactionService service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(AppointmentOrderMapper.class);
        routeMapper = mock(CampRouteMapper.class);
        inventoryTaskService = mock(InventoryTaskService.class);
        service = new OrderCancellationTransactionService(orderMapper, routeMapper, inventoryTaskService);
    }

    @Test
    void shouldRestoreMysqlExactlyOnceAfterSuccessfulStatusCas() {
        AppointmentOrder order = order(11L, 8L);
        when(orderMapper.selectById(11L)).thenReturn(order);
        when(orderMapper.cancelWaitPay(11L, null)).thenReturn(1);
        when(routeMapper.increaseStock(8L, 1)).thenReturn(1);

        CancelledOrder cancelled = service.cancelWaitPay(11L, null);

        assertThat(cancelled.cancelled()).isTrue();
        assertThat(cancelled.routeId()).isEqualTo(8L);
        verify(routeMapper).increaseStock(8L, 1);
        verify(inventoryTaskService).createOrderReleaseTask(11L, 8L);
    }

    @Test
    void shouldDoNothingWhenAnotherWorkerAlreadyChangedStatus() {
        when(orderMapper.selectById(11L)).thenReturn(order(11L, 8L));
        when(orderMapper.cancelWaitPay(11L, null)).thenReturn(0);

        CancelledOrder cancelled = service.cancelWaitPay(11L, null);

        assertThat(cancelled.cancelled()).isFalse();
        verify(routeMapper, never()).increaseStock(8L, 1);
        verify(inventoryTaskService, never()).createOrderReleaseTask(11L, 8L);
    }

    @Test
    void shouldAbortTransactionWhenMysqlStockCannotBeRestored() {
        when(orderMapper.selectById(11L)).thenReturn(order(11L, 8L));
        when(orderMapper.cancelWaitPay(11L, null)).thenReturn(1);
        when(routeMapper.increaseStock(8L, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.cancelWaitPay(11L, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVENTORY_OPERATION_FAILED));
    }

    private AppointmentOrder order(long orderId, long routeId) {
        AppointmentOrder order = new AppointmentOrder();
        order.setId(orderId);
        order.setRouteId(routeId);
        return order;
    }
}
