package com.nfu.pettravel.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Appointment order lifecycle. Persisted codes are part of the database contract.
 */
public enum OrderStatus {
    WAIT_PAY(0),
    PAID(1),
    CANCELLED(2),
    COMPLETED(3);

    @EnumValue
    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    public static OrderStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown order status code: " + code));
    }

    public boolean canTransitionTo(OrderStatus target) {
        Set<OrderStatus> allowed = switch (this) {
            case WAIT_PAY -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(COMPLETED);
            case CANCELLED, COMPLETED -> EnumSet.noneOf(OrderStatus.class);
        };
        return allowed.contains(target);
    }
}
