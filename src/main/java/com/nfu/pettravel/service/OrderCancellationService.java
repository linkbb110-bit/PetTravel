package com.nfu.pettravel.service;

import com.nfu.pettravel.cache.RouteCacheService;
import com.nfu.pettravel.inventory.InventoryGateway;
import com.nfu.pettravel.service.impl.OrderCancellationTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderCancellationService {
    private final OrderCancellationTransactionService transactionService;
    private final InventoryGateway inventoryGateway;
    private final InventoryTaskService inventoryTaskService;
    private final RouteCacheService routeCacheService;

    public OrderCancellationService(
            OrderCancellationTransactionService transactionService,
            InventoryGateway inventoryGateway,
            InventoryTaskService inventoryTaskService,
            RouteCacheService routeCacheService) {
        this.transactionService = transactionService;
        this.inventoryGateway = inventoryGateway;
        this.inventoryTaskService = inventoryTaskService;
        this.routeCacheService = routeCacheService;
    }

    public boolean cancel(long orderId, Long expectedUserId) {
        CancelledOrder result = transactionService.cancelWaitPay(orderId, expectedUserId);
        if (!result.cancelled()) {
            return false;
        }
        routeCacheService.evict();
        try {
            inventoryGateway.releaseOrder(result.routeId(), result.orderId(), 1);
            inventoryTaskService.markOrderReleased(orderId);
        } catch (RuntimeException releaseFailure) {
            // MySQL task was committed together with cancellation; scheduled recovery will retry Redis.
            log.warn("Redis order release deferred: orderId={}, cause={}",
                    orderId, releaseFailure.toString());
        }
        return true;
    }
}
