package com.nfu.pettravel.service;

public interface InventoryTaskService {
    void createReservationConfirmTask(long orderId, long routeId);

    void markReservationConfirmed(long orderId);

    void recordBookingRollback(long orderId, long routeId, String reason);

    void createOrderReleaseTask(long orderId, long routeId);

    void markOrderReleased(long orderId);

    void requestRouteReconciliation(long routeId);
}
