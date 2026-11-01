package com.nfu.pettravel.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookRouteRequest(
        @NotNull(message = "请选择出游宠物")
        @Positive(message = "宠物ID必须为正数")
        Long petId) {
}
