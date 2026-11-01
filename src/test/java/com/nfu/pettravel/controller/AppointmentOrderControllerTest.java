package com.nfu.pettravel.controller;

import com.nfu.pettravel.common.ApiResponse;
import com.nfu.pettravel.enums.UserRole;
import com.nfu.pettravel.service.AppointmentOrderService;
import com.nfu.pettravel.service.OrderCancellationService;
import com.nfu.pettravel.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentOrderControllerTest {

    @AfterEach
    void clearContext() {
        UserContext.removeUserId();
    }

    @Test
    void shouldExposeWhetherUserCancellationActuallyWonTheCas() {
        AppointmentOrderService orderService = mock(AppointmentOrderService.class);
        OrderCancellationService cancellationService = mock(OrderCancellationService.class);
        AppointmentOrderController controller =
                new AppointmentOrderController(orderService, cancellationService);
        UserContext.setUser(9L, UserRole.USER);
        when(cancellationService.cancel(11L, 9L)).thenReturn(false);

        ApiResponse<Boolean> response = controller.cancel(11L);

        assertThat(response.data()).isFalse();
    }
}
