package com.nfu.pettravel.service;

public record CancelledOrder(boolean cancelled, long orderId, long routeId) {
    public static CancelledOrder alreadyHandled(long orderId, long routeId) {
        return new CancelledOrder(false, orderId, routeId);
    }

    public static CancelledOrder cancelled(long orderId, long routeId) {
        return new CancelledOrder(true, orderId, routeId);
    }
}
