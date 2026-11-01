package com.nfu.pettravel.controller;

import com.nfu.pettravel.common.ApiResponse;
import com.nfu.pettravel.common.annotation.Log;
import com.nfu.pettravel.utils.AliOssUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/common")
public class CommonController {
    private final AliOssUtil aliOssUtil;

    public CommonController(AliOssUtil aliOssUtil) {
        this.aliOssUtil = aliOssUtil;
    }

    @Log(title = "上传宠物图片")
    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(aliOssUtil.upload(file));
    }
}
