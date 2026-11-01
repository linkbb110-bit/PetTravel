package com.nfu.pettravel.controller;

import com.nfu.pettravel.common.ApiResponse;
import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.common.annotation.AdminOnly;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.service.AppointmentOrderService;
import com.nfu.pettravel.service.OrderCancellationService;
import com.nfu.pettravel.utils.UserContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/order")
@Validated
public class AppointmentOrderController {
    private final AppointmentOrderService orderService;
    private final OrderCancellationService cancellationService;

    public AppointmentOrderController(
            AppointmentOrderService orderService,
            OrderCancellationService cancellationService) {
        this.orderService = orderService;
        this.cancellationService = cancellationService;
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<Void> pay(@PathVariable @Positive long orderId) {
        orderService.pay(orderId, currentUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Boolean> cancel(@PathVariable @Positive long orderId) {
        return ApiResponse.success(cancellationService.cancel(orderId, currentUserId()));
    }

    @PostMapping("/{orderId}/complete")
    @AdminOnly
    public ApiResponse<Void> complete(@PathVariable @Positive long orderId) {
        orderService.complete(orderId);
        return ApiResponse.success();
    }

    @GetMapping("/export/timeout")
    @AdminOnly
    public void exportTimeoutOrders(HttpServletResponse response) throws IOException {
        orderService.exportTimeoutOrders(response);
    }

    private long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
