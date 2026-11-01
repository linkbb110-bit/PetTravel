package com.nfu.pettravel.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum InventoryTaskType {
    BOOKING_ROLLBACK("BOOKING_ROLLBACK"),
    ORDER_RELEASE("ORDER_RELEASE"),
    RESERVATION_CONFIRM("RESERVATION_CONFIRM"),
    ROUTE_RECONCILE("ROUTE_RECONCILE");

    @EnumValue
    private final String value;

    InventoryTaskType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
