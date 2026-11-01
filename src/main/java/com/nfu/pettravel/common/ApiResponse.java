package com.nfu.pettravel.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(0, "success", null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }
}
