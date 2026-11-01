package com.nfu.pettravel.service;

import com.nfu.pettravel.websocket.AdminWebSocketHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OrderNotificationService {
    private final AdminWebSocketHandler webSocketHandler;

    public OrderNotificationService(AdminWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Async("orderExecutor")
    public void notifyNewOrder(long orderId) {
        webSocketHandler.sendToAllAdmins("NEW_ORDER:" + orderId);
    }
}
