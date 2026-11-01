package com.nfu.pettravel.service;

import com.nfu.pettravel.dto.BookRouteResponse;
import com.nfu.pettravel.cache.RouteCacheService;
import com.nfu.pettravel.inventory.InventoryGateway;
import com.nfu.pettravel.inventory.InventoryReservationResult;
import com.nfu.pettravel.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.core.task.TaskRejectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingOrchestratorTest {
    private InventoryGateway inventoryGateway;
    private BookingTransactionService bookingTransactionService;
    private InventoryTaskService inventoryTaskService;
    private BookingOrchestrator orchestrator;
    private RouteCacheService routeCacheService;
    private OrderNotificationService notificationService;

    @BeforeEach
    void setUp() {
        inventoryGateway = mock(InventoryGateway.class);
        bookingTransactionService = mock(BookingTransactionService.class);
        inventoryTaskService = mock(InventoryTaskService.class);
        routeCacheService = mock(RouteCacheService.class);
        notificationService = mock(OrderNotificationService.class);
        orchestrator = new BookingOrchestrator(
                inventoryGateway, bookingTransactionService, inventoryTaskService,
                routeCacheService, notificationService);
    }

    @Test
    void shouldReserveRedisThenCommitMysqlThenConfirmReservation() {
        when(inventoryGateway.reserve(8L, 101L, 1)).thenReturn(new InventoryReservationResult(9));
        when(bookingTransactionService.createBooking(101L, 3L, 8L, 5L)).thenReturn(101L);

        BookRouteResponse response = orchestrator.bookWithOrderId(101L, 3L, 8L, 5L);

        assertThat(response.orderId()).isEqualTo(101L);
        assertThat(response.remainingStock()).isEqualTo(9);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.WAIT_PAY);
        InOrder order = inOrder(inventoryGateway, bookingTransactionService);
        order.verify(inventoryGateway).reserve(8L, 101L, 1);
        order.verify(bookingTransactionService).createBooking(101L, 3L, 8L, 5L);
        order.verify(inventoryGateway).confirm(8L, 101L);
        verify(inventoryTaskService).markReservationConfirmed(101L);
        verify(routeCacheService).evict();
        verify(notificationService).notifyNewOrder(101L);
    }

    @Test
    void shouldCompensateRedisWhenMysqlTransactionFails() {
        when(inventoryGateway.reserve(8L, 102L, 1)).thenReturn(new InventoryReservationResult(9));
        when(bookingTransactionService.createBooking(102L, 3L, 8L, 5L))
                .thenThrow(new IllegalStateException("insert failed"));

        assertThatThrownBy(() -> orchestrator.bookWithOrderId(102L, 3L, 8L, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("insert failed");

        verify(inventoryGateway).compensateReservation(8L, 102L);
        verify(inventoryGateway, never()).confirm(8L, 102L);
    }

    @Test
    void shouldLeaveDurableConfirmTaskPendingWhenRedisConfirmTemporarilyFails() {
        when(inventoryGateway.reserve(8L, 103L, 1)).thenReturn(new InventoryReservationResult(9));
        when(bookingTransactionService.createBooking(103L, 3L, 8L, 5L)).thenReturn(103L);
        when(inventoryGateway.confirm(8L, 103L)).thenThrow(new IllegalStateException("redis down"));

        BookRouteResponse response = orchestrator.bookWithOrderId(103L, 3L, 8L, 5L);

        assertThat(response.orderId()).isEqualTo(103L);
        verify(inventoryTaskService, never()).markReservationConfirmed(103L);
        verify(inventoryGateway, never()).compensateReservation(8L, 103L);
    }

    @Test
    void shouldNotFailCommittedBookingWhenNotificationExecutorIsSaturated() {
        when(inventoryGateway.reserve(8L, 104L, 1)).thenReturn(new InventoryReservationResult(9));
        when(bookingTransactionService.createBooking(104L, 3L, 8L, 5L)).thenReturn(104L);
        org.mockito.Mockito.doThrow(new TaskRejectedException("executor full"))
                .when(notificationService).notifyNewOrder(104L);

        BookRouteResponse response = orchestrator.bookWithOrderId(104L, 3L, 8L, 5L);

        assertThat(response.orderId()).isEqualTo(104L);
    }
}
