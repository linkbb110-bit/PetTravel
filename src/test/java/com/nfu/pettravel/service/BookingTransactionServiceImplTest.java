package com.nfu.pettravel.service;

import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.entity.AppointmentOrder;
import com.nfu.pettravel.entity.CampRoute;
import com.nfu.pettravel.entity.PetInfo;
import com.nfu.pettravel.enums.OrderStatus;
import com.nfu.pettravel.enums.VaccineStatus;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.mapper.AppointmentOrderMapper;
import com.nfu.pettravel.mapper.CampRouteMapper;
import com.nfu.pettravel.mapper.PetInfoMapper;
import com.nfu.pettravel.service.impl.BookingTransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingTransactionServiceImplTest {
    private AppointmentOrderMapper orderMapper;
    private CampRouteMapper routeMapper;
    private PetInfoMapper petMapper;
    private InventoryTaskService inventoryTaskService;
    private BookingTransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(AppointmentOrderMapper.class);
        routeMapper = mock(CampRouteMapper.class);
        petMapper = mock(PetInfoMapper.class);
        inventoryTaskService = mock(InventoryTaskService.class);
        service = new BookingTransactionServiceImpl(orderMapper, routeMapper, petMapper, inventoryTaskService);
    }

    @Test
    void shouldAtomicallyDecreaseMysqlAndInsertWaitPayOrder() {
        PetInfo pet = eligiblePet(5L, 3L);
        CampRoute route = openRoute(8L);
        when(petMapper.selectById(5L)).thenReturn(pet);
        when(routeMapper.selectById(8L)).thenReturn(route);
        when(orderMapper.countActiveBooking(3L, 8L)).thenReturn(0L);
        when(routeMapper.decreaseStock(8L, 1)).thenReturn(1);
        when(orderMapper.insert(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        long orderId = service.createBooking(101L, 3L, 8L, 5L);

        assertThat(orderId).isEqualTo(101L);
        ArgumentCaptor<AppointmentOrder> captor = ArgumentCaptor.forClass(AppointmentOrder.class);
        verify(orderMapper).insert(captor.capture());
        assertThat(captor.getValue().getOrderStatus()).isEqualTo(OrderStatus.WAIT_PAY);
        assertThat(captor.getValue().getPetId()).isEqualTo(5L);
        verify(inventoryTaskService).createReservationConfirmTask(101L, 8L);
    }

    @Test
    void shouldRejectPetThatDoesNotBelongToCurrentUser() {
        when(petMapper.selectById(5L)).thenReturn(eligiblePet(5L, 99L));

        assertBusinessError(() -> service.createBooking(101L, 3L, 8L, 5L), ErrorCode.PET_NOT_OWNED);

        verify(routeMapper, never()).decreaseStock(8L, 1);
    }

    @Test
    void shouldRejectIncompleteVaccination() {
        PetInfo pet = eligiblePet(5L, 3L);
        pet.setVaccineStatus(VaccineStatus.INCOMPLETE);
        when(petMapper.selectById(5L)).thenReturn(pet);

        assertBusinessError(() -> service.createBooking(101L, 3L, 8L, 5L), ErrorCode.PET_NOT_ELIGIBLE);
    }

    @Test
    void shouldRejectDuplicateActiveBookingBeforeStockMutation() {
        when(petMapper.selectById(5L)).thenReturn(eligiblePet(5L, 3L));
        when(routeMapper.selectById(8L)).thenReturn(openRoute(8L));
        when(orderMapper.countActiveBooking(3L, 8L)).thenReturn(1L);

        assertBusinessError(() -> service.createBooking(101L, 3L, 8L, 5L), ErrorCode.DUPLICATE_BOOKING);
        verify(routeMapper, never()).decreaseStock(8L, 1);
    }

    @Test
    void shouldFailWhenAtomicMysqlDecrementAffectsNoRows() {
        when(petMapper.selectById(5L)).thenReturn(eligiblePet(5L, 3L));
        when(routeMapper.selectById(8L)).thenReturn(openRoute(8L));
        when(orderMapper.countActiveBooking(3L, 8L)).thenReturn(0L);
        when(routeMapper.decreaseStock(8L, 1)).thenReturn(0);

        assertBusinessError(() -> service.createBooking(101L, 3L, 8L, 5L), ErrorCode.STOCK_INSUFFICIENT);
        verify(orderMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    private PetInfo eligiblePet(long petId, long ownerId) {
        PetInfo pet = new PetInfo();
        pet.setId(petId);
        pet.setUserId(ownerId);
        pet.setVaccineStatus(VaccineStatus.COMPLETE);
        pet.setIsDeleted(0);
        return pet;
    }

    private CampRoute openRoute(long routeId) {
        CampRoute route = new CampRoute();
        route.setId(routeId);
        route.setStatus(1);
        route.setIsDeleted(0);
        return route;
    }

    private void assertBusinessError(Runnable operation, ErrorCode expected) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
