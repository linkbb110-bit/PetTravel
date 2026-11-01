package com.nfu.pettravel.controller;

import com.nfu.pettravel.common.ApiResponse;
import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.common.annotation.Log;
import com.nfu.pettravel.dto.UserLoginDTO;
import com.nfu.pettravel.dto.UserRegisterDTO;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.service.SysUserService;
import com.nfu.pettravel.utils.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class SysUserController {
    private final SysUserService sysUserService;

    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @PostMapping("/login")
    public ApiResponse<String> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        return ApiResponse.success(sysUserService.login(loginDTO));
    }

    @PostMapping("/register")
    public ApiResponse<Long> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        return ApiResponse.success(sysUserService.register(registerDTO));
    }

    @Log(title = "获取当前登录用户信息")
    @GetMapping("/me")
    public ApiResponse<Long> getMyInfo() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.success(userId);
    }
}
