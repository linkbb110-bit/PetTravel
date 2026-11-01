package com.nfu.pettravel.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_ARGUMENT(40000, "请求参数不合法", HttpStatus.BAD_REQUEST),
    STOCK_INSUFFICIENT(40001, "库存不足", HttpStatus.CONFLICT),
    INVENTORY_NOT_INITIALIZED(40002, "库存尚未初始化", HttpStatus.SERVICE_UNAVAILABLE),
    INVENTORY_BUSY(40003, "库存正在校准，请稍后重试", HttpStatus.CONFLICT),
    PET_NOT_ELIGIBLE(40004, "宠物疫苗资格不满足预约要求", HttpStatus.BAD_REQUEST),
    FILE_INVALID(40005, "上传文件不合法", HttpStatus.BAD_REQUEST),
    USERNAME_EXISTS(40006, "用户名已存在", HttpStatus.CONFLICT),
    UNAUTHORIZED(40100, "请先登录", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(40101, "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "无权执行此操作", HttpStatus.FORBIDDEN),
    PET_NOT_OWNED(40301, "宠物档案不属于当前用户", HttpStatus.FORBIDDEN),
    USER_DISABLED(40302, "用户已被禁用", HttpStatus.FORBIDDEN),
    PET_NOT_FOUND(40401, "宠物档案不存在", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(40402, "订单不存在", HttpStatus.NOT_FOUND),
    ROUTE_NOT_FOUND(40403, "路线不存在", HttpStatus.NOT_FOUND),
    DUPLICATE_BOOKING(40901, "该路线已有未结束的预约", HttpStatus.CONFLICT),
    ORDER_STATUS_INVALID(40902, "当前订单状态不允许该操作", HttpStatus.CONFLICT),
    ROUTE_UNAVAILABLE(40903, "路线当前不可预约", HttpStatus.CONFLICT),
    INTERNAL_ERROR(50000, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR),
    INVENTORY_OPERATION_FAILED(50001, "库存处理失败，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR),
    UPLOAD_FAILED(50002, "文件上传失败", HttpStatus.INTERNAL_SERVER_ERROR),
    EXPORT_FAILED(50003, "订单导出失败", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
