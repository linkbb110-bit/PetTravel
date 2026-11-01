package com.nfu.pettravel.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum InventoryTaskStatus {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    SUCCEEDED("SUCCEEDED"),
    NEEDS_RECONCILE("NEEDS_RECONCILE");

    @EnumValue
    private final String value;

    InventoryTaskStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
