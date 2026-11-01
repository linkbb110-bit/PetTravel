package com.nfu.pettravel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RouteUpsertRequest(
        @NotBlank(message = "路线标题不能为空")
        @Size(max = 100, message = "路线标题长度不能超过100")
        String title,
        @NotNull(message = "路线价格不能为空")
        @DecimalMin(value = "0.00", message = "路线价格不能为负数")
        BigDecimal price,
        @NotNull(message = "总库存不能为空")
        @Min(value = 0, message = "总库存不能为负数")
        Integer totalStock,
        @NotNull(message = "路线状态不能为空")
        @Min(value = 0, message = "路线状态不合法")
        @Max(value = 1, message = "路线状态不合法")
        Integer status) {
}
