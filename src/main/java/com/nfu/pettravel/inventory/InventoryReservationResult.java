package com.nfu.pettravel.inventory;

import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.exception.BusinessException;

public record InventoryReservationResult(long remainingStock) {

    public static InventoryReservationResult fromLuaResult(long result) {
        if (result >= 0) {
            return new InventoryReservationResult(result);
        }
        ErrorCode errorCode = switch ((int) result) {
            case -1 -> ErrorCode.STOCK_INSUFFICIENT;
            case -2 -> ErrorCode.INVENTORY_NOT_INITIALIZED;
            case -3 -> ErrorCode.INVENTORY_BUSY;
            default -> ErrorCode.INVENTORY_OPERATION_FAILED;
        };
        throw new BusinessException(errorCode);
    }
}
