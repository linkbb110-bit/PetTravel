package com.nfu.pettravel.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.pettravel.cache.RouteCacheService;
import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.dto.BookRouteResponse;
import com.nfu.pettravel.dto.RouteUpsertRequest;
import com.nfu.pettravel.entity.CampRoute;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.mapper.CampRouteMapper;
import com.nfu.pettravel.service.BookingOrchestrator;
import com.nfu.pettravel.service.CampRouteService;
import com.nfu.pettravel.service.InventoryReconciliationService;
import com.nfu.pettravel.service.InventoryTaskService;
import com.nfu.pettravel.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CampRouteServiceImpl extends ServiceImpl<CampRouteMapper, CampRoute>
        implements CampRouteService {
    private final CampRouteMapper routeMapper;
    private final RouteCacheService routeCacheService;
    private final BookingOrchestrator bookingOrchestrator;
    private final InventoryReconciliationService reconciliationService;
    private final InventoryTaskService inventoryTaskService;

    public CampRouteServiceImpl(
            CampRouteMapper routeMapper,
            RouteCacheService routeCacheService,
            BookingOrchestrator bookingOrchestrator,
            InventoryReconciliationService reconciliationService,
            InventoryTaskService inventoryTaskService) {
        this.routeMapper = routeMapper;
        this.routeCacheService = routeCacheService;
        this.bookingOrchestrator = bookingOrchestrator;
        this.reconciliationService = reconciliationService;
        this.inventoryTaskService = inventoryTaskService;
    }

    @Override
    public List<CampRoute> getHotRoutesFromCache() {
        return routeCacheService.getHotRoutes(() -> routeMapper.selectList(
                Wrappers.<CampRoute>lambdaQuery()
                        .eq(CampRoute::getStatus, 1)
                        .eq(CampRoute::getIsDeleted, 0)
                        .orderByDesc(CampRoute::getCreateTime)));
    }

    @Override
    public void initRouteStockToRedis() {
        List<CampRoute> routes = routeMapper.selectList(Wrappers.<CampRoute>lambdaQuery()
                .eq(CampRoute::getStatus, 1)
                .eq(CampRoute::getIsDeleted, 0));
        for (CampRoute route : routes) {
            inventoryTaskService.requestRouteReconciliation(route.getId());
            tryReconcileRoute(route.getId());
        }
    }

    @Override
    public BookRouteResponse bookRoute(Long routeId, Long petId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return bookingOrchestrator.book(userId, routeId, petId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createRoute(RouteUpsertRequest request) {
        LocalDateTime now = LocalDateTime.now();
        CampRoute route = new CampRoute();
        route.setTitle(request.title());
        route.setPrice(request.price());
        route.setTotalStock(request.totalStock());
        route.setAvailableStock(request.totalStock());
        route.setVersion(0);
        route.setStatus(request.status());
        route.setCreateTime(now);
        route.setUpdateTime(now);
        route.setIsDeleted(0);
        if (routeMapper.insert(route) != 1 || route.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        inventoryTaskService.requestRouteReconciliation(route.getId());
        routeCacheService.evictAfterCommit();
        reconcileAfterCommit(route.getId());
        return route.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoute(long routeId, RouteUpsertRequest request) {
        int affected = routeMapper.updateDefinition(
                routeId, request.title(), request.price(), request.totalStock(), request.status());
        if (affected != 1) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "路线不存在，或总库存小于已预约数量");
        }
        inventoryTaskService.requestRouteReconciliation(routeId);
        routeCacheService.evictAfterCommit();
        reconcileAfterCommit(routeId);
    }

    private void reconcileAfterCommit(long routeId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            tryReconcileRoute(routeId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                tryReconcileRoute(routeId);
            }
        });
    }

    private void tryReconcileRoute(long routeId) {
        try {
            reconciliationService.reconcileAndMarkRequested(routeId);
        } catch (RuntimeException failure) {
            // The durable ROUTE_RECONCILE task was persisted before this best-effort call.
            log.warn("Route inventory reconciliation deferred: routeId={}, cause={}",
                    routeId, failure.toString());
        }
    }
}
