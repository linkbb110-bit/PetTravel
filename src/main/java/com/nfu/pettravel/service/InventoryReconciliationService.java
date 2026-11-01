package com.nfu.pettravel.service;

import com.nfu.pettravel.enums.InventoryTaskType;
import com.nfu.pettravel.inventory.InventoryGateway;
import com.nfu.pettravel.mapper.CampRouteMapper;
import com.nfu.pettravel.mapper.InventoryCompensationTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
public class InventoryReconciliationService {
    private final InventoryGateway inventoryGateway;
    private final CampRouteMapper routeMapper;
    private final InventoryCompensationTaskMapper taskMapper;

    public InventoryReconciliationService(
            InventoryGateway inventoryGateway,
            CampRouteMapper routeMapper,
            InventoryCompensationTaskMapper taskMapper) {
        this.inventoryGateway = inventoryGateway;
        this.routeMapper = routeMapper;
        this.taskMapper = taskMapper;
    }

    /**
     * Redis is locked before MySQL is read. The route row lock then serializes this
     * snapshot with booking, cancellation, and capacity updates.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean reconcile(long routeId) {
        return reconcileLocked(routeId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean reconcileAndMarkRequested(long routeId) {
        boolean reconciled = reconcileLocked(routeId);
        if (reconciled) {
            taskMapper.markSucceeded(routeId, InventoryTaskType.ROUTE_RECONCILE.getValue());
        }
        return reconciled;
    }

    private boolean reconcileLocked(long routeId) {
        String token = inventoryGateway.tryAcquireReconciliationLock(routeId);
        if (token == null) {
            return false;
        }
        boolean unlockDeferred = deferUnlockUntilTransactionCompletion(routeId, token);
        try {
            Integer databaseStock = routeMapper.selectAvailableStockForUpdate(routeId);
            if (databaseStock == null || taskMapper.countBlockingByRoute(routeId) > 0) {
                return false;
            }
            return inventoryGateway.synchronizeStockUnderLock(routeId, token, databaseStock);
        } finally {
            if (!unlockDeferred) {
                releaseLockQuietly(routeId, token);
            }
        }
    }

    private boolean deferUnlockUntilTransactionCompletion(long routeId, String token) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                releaseLockQuietly(routeId, token);
            }
        });
        return true;
    }

    private void releaseLockQuietly(long routeId, String token) {
        try {
            inventoryGateway.releaseReconciliationLock(routeId, token);
        } catch (RuntimeException unlockFailure) {
            // The lock has a bounded TTL; unlock failure must not rewrite the completed DB outcome.
            log.warn("Inventory reconciliation lock release deferred to TTL: routeId={}, cause={}",
                    routeId, unlockFailure.toString());
        }
    }
}
