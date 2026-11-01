package com.nfu.pettravel.service.impl;

import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.entity.AppointmentOrder;
import com.nfu.pettravel.entity.CampRoute;
import com.nfu.pettravel.entity.PetInfo;
import com.nfu.pettravel.enums.OrderStatus;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.mapper.AppointmentOrderMapper;
import com.nfu.pettravel.mapper.CampRouteMapper;
import com.nfu.pettravel.mapper.PetInfoMapper;
import com.nfu.pettravel.service.BookingTransactionService;
import com.nfu.pettravel.service.InventoryTaskService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BookingTransactionServiceImpl implements BookingTransactionService {
    private final AppointmentOrderMapper orderMapper;
    private final CampRouteMapper routeMapper;
    private final PetInfoMapper petMapper;
    private final InventoryTaskService inventoryTaskService;

    public BookingTransactionServiceImpl(
            AppointmentOrderMapper orderMapper,
            CampRouteMapper routeMapper,
            PetInfoMapper petMapper,
            InventoryTaskService inventoryTaskService) {
        this.orderMapper = orderMapper;
        this.routeMapper = routeMapper;
        this.petMapper = petMapper;
        this.inventoryTaskService = inventoryTaskService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createBooking(long orderId, long userId, long routeId, long petId) {
        validatePet(userId, petId);
        validateRoute(routeId);
        if (orderMapper.countActiveBooking(userId, routeId) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_BOOKING);
        }
        if (routeMapper.decreaseStock(routeId, 1) != 1) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
        }

        LocalDateTime now = LocalDateTime.now();
        AppointmentOrder order = new AppointmentOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setPetId(petId);
        order.setRouteId(routeId);
        order.setOrderStatus(OrderStatus.WAIT_PAY);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order.setIsDeleted(0);
        try {
            if (orderMapper.insert(order) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        } catch (DuplicateKeyException duplicate) {
            throw new BusinessException(ErrorCode.DUPLICATE_BOOKING);
        }
        inventoryTaskService.createReservationConfirmTask(orderId, routeId);
        return orderId;
    }

    private void validatePet(long userId, long petId) {
        PetInfo pet = petMapper.selectById(petId);
        if (pet == null || Integer.valueOf(1).equals(pet.getIsDeleted())) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }
        if (!Long.valueOf(userId).equals(pet.getUserId())) {
            throw new BusinessException(ErrorCode.PET_NOT_OWNED);
        }
        if (pet.getVaccineStatus() == null || !pet.getVaccineStatus().isEligible()) {
            throw new BusinessException(ErrorCode.PET_NOT_ELIGIBLE);
        }
    }

    private void validateRoute(long routeId) {
        CampRoute route = routeMapper.selectById(routeId);
        if (route == null || Integer.valueOf(1).equals(route.getIsDeleted())) {
            throw new BusinessException(ErrorCode.ROUTE_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(route.getStatus())) {
            throw new BusinessException(ErrorCode.ROUTE_UNAVAILABLE);
        }
    }
}
