package com.nfu.pettravel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegisterDTO(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度必须为3到50")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 100, message = "密码长度必须为8到100")
        String password,
        @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
        String phone) {
}
