package com.nfu.pettravel.inventory;

import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryReservationResultTest {

    @Test
    void shouldExposeRemainingStockForSuccessfulReservation() {
        assertThat(InventoryReservationResult.fromLuaResult(17).remainingStock()).isEqualTo(17);
    }

    @Test
    void shouldMapLuaFailuresToStableBusinessErrors() {
        assertFailure(-1, ErrorCode.STOCK_INSUFFICIENT);
        assertFailure(-2, ErrorCode.INVENTORY_NOT_INITIALIZED);
        assertFailure(-3, ErrorCode.INVENTORY_BUSY);
    }

    private void assertFailure(long luaResult, ErrorCode expected) {
        assertThatThrownBy(() -> InventoryReservationResult.fromLuaResult(luaResult))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
