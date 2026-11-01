package com.nfu.pettravel.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VaccineStatus {
    INCOMPLETE(0),
    COMPLETE(1);

    @EnumValue
    private final int code;

    VaccineStatus(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    public boolean isEligible() {
        return this == COMPLETE;
    }
}
