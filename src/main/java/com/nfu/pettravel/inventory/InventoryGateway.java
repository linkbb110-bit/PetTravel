package com.nfu.pettravel.inventory;

import java.util.Set;

public interface InventoryGateway {
    InventoryReservationResult reserve(long routeId, long orderId, int quantity);

    boolean confirm(long routeId, long orderId);

    boolean compensateReservation(long routeId, long orderId);

    boolean releaseOrder(long routeId, long orderId, int quantity);

    String tryAcquireReconciliationLock(long routeId);

    boolean synchronizeStockUnderLock(long routeId, String lockToken, int databaseStock);

    void releaseReconciliationLock(long routeId, String lockToken);

    Long getStock(long routeId);

    Set<Long> findExpiredReservations(long routeId, long deadlineEpochMillis, int limit);
}
