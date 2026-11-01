package com.nfu.pettravel.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nfu.pettravel.cache.RouteCacheService;
import com.nfu.pettravel.dto.BookRouteResponse;
import com.nfu.pettravel.enums.OrderStatus;
import com.nfu.pettravel.inventory.InventoryGateway;
import com.nfu.pettravel.inventory.InventoryReservationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BookingOrchestrator {
    private final InventoryGateway inventoryGateway;
    private final BookingTransactionService bookingTransactionService;
    private final InventoryTaskService inventoryTaskService;
    private final RouteCacheService routeCacheService;
    private final OrderNotificationService notificationService;

    public BookingOrchestrator(
            InventoryGateway inventoryGateway,
            BookingTransactionService bookingTransactionService,
            InventoryTaskService inventoryTaskService,
            RouteCacheService routeCacheService,
            OrderNotificationService notificationService) {
        this.inventoryGateway = inventoryGateway;
        this.bookingTransactionService = bookingTransactionService;
        this.inventoryTaskService = inventoryTaskService;
        this.routeCacheService = routeCacheService;
        this.notificationService = notificationService;
    }

    public BookRouteResponse book(long userId, long routeId, long petId) {
        return bookWithOrderId(IdWorker.getId(), userId, routeId, petId);
    }

    BookRouteResponse bookWithOrderId(long orderId, long userId, long routeId, long petId) {
        InventoryReservationResult reservation = inventoryGateway.reserve(routeId, orderId, 1);
        try {
            bookingTransactionService.createBooking(orderId, userId, routeId, petId);
        } catch (RuntimeException databaseFailure) {
            compensateFailedBooking(orderId, routeId, databaseFailure);
            throw databaseFailure;
        }
        routeCacheService.evict();

        try {
            inventoryGateway.confirm(routeId, orderId);
            inventoryTaskService.markReservationConfirmed(orderId);
        } catch (RuntimeException confirmFailure) {
            // MySQL has committed. The durable RESERVATION_CONFIRM task must retry; never roll back stock here.
            log.warn("Redis reservation confirmation deferred: orderId={}, cause={}",
                    orderId, confirmFailure.toString());
        }
        try {
            notificationService.notifyNewOrder(orderId);
        } catch (RuntimeException notificationFailure) {
            // Notification is explicitly non-critical; executor saturation must not fail a committed order.
            log.warn("New-order notification dropped: orderId={}, cause={}",
                    orderId, notificationFailure.toString());
        }
        return new BookRouteResponse(orderId, reservation.remainingStock(), OrderStatus.WAIT_PAY);
    }

    private void compensateFailedBooking(long orderId, long routeId, RuntimeException databaseFailure) {
        try {
            inventoryGateway.compensateReservation(routeId, orderId);
        } catch (RuntimeException compensationFailure) {
            databaseFailure.addSuppressed(compensationFailure);
            try {
                inventoryTaskService.recordBookingRollback(orderId, routeId, summarize(databaseFailure));
            } catch (RuntimeException taskFailure) {
                databaseFailure.addSuppressed(taskFailure);
                // The Redis pending-reservation set is the second recovery source when MySQL is unavailable.
                log.error("Failed to persist inventory rollback task: orderId={}, cause={}",
                        orderId, taskFailure.toString());
            }
        }
    }

    private String summarize(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
