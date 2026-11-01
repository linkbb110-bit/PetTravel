package com.nfu.pettravel.dto;

import com.nfu.pettravel.enums.VaccineStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PetInfoRequest(
        @NotBlank(message = "宠物昵称不能为空")
        @Size(max = 50, message = "宠物昵称长度不能超过50")
        String name,
        @NotBlank(message = "宠物品种不能为空")
        @Size(max = 50, message = "宠物品种长度不能超过50")
        String breed,
        @NotNull(message = "宠物年龄不能为空")
        @Min(value = 0, message = "宠物年龄不能为负数")
        @Max(value = 600, message = "宠物年龄超出允许范围")
        Integer age,
        @Size(max = 500, message = "头像地址长度不能超过500")
        String avatarUrl,
        @NotNull(message = "请选择疫苗状态")
        VaccineStatus vaccineStatus,
        @Size(max = 1000, message = "性格标签内容过长")
        String characterTags) {
}
