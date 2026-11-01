package com.nfu.pettravel.dto;

import com.nfu.pettravel.enums.OrderStatus;

public record BookRouteResponse(long orderId, long remainingStock, OrderStatus orderStatus) {
}
