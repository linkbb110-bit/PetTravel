package com.nfu.pettravel.task;

import com.nfu.pettravel.mapper.AppointmentOrderMapper;
import com.nfu.pettravel.service.OrderCancellationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTimeoutTask {
    private final AppointmentOrderMapper orderMapper;
    private final OrderCancellationService cancellationService;
    private final Duration paymentTimeout;
    private final int batchSize;

    public OrderTimeoutTask(
            AppointmentOrderMapper orderMapper,
            OrderCancellationService cancellationService,
            @Value("${pettravel.order.payment-timeout:15m}") Duration paymentTimeout,
            @Value("${pettravel.order.timeout-batch-size:200}") int batchSize) {
        this.orderMapper = orderMapper;
        this.cancellationService = cancellationService;
        this.paymentTimeout = paymentTimeout;
        this.batchSize = batchSize;
    }

    @Scheduled(
            fixedDelayString = "${pettravel.order.timeout-scan-delay:60000}",
            initialDelayString = "${pettravel.order.timeout-scan-initial-delay:30000}")
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minus(paymentTimeout);
        List<Long> orderIds = orderMapper.findTimeoutOrderIds(deadline, batchSize);
        int cancelled = 0;
        for (Long orderId : orderIds) {
            try {
                if (cancellationService.cancel(orderId, null)) {
                    cancelled++;
                }
            } catch (RuntimeException exception) {
                log.error("Failed to cancel timeout order: orderId={}", orderId, exception);
            }
        }
        if (cancelled > 0) {
            log.info("Cancelled {} timeout orders", cancelled);
        }
    }
}
