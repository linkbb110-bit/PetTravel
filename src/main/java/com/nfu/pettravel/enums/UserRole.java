package com.nfu.pettravel.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    USER(0),
    ADMIN(1);

    @EnumValue
    private final int code;

    UserRole(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    public static UserRole fromCode(int code) {
        for (UserRole role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown user role code: " + code);
    }
}
