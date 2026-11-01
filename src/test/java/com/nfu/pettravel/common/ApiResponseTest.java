package com.nfu.pettravel.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void shouldBuildStableSuccessAndFailureContracts() {
        ApiResponse<String> success = ApiResponse.success("order-id");
        ApiResponse<Void> failure = ApiResponse.failure(ErrorCode.STOCK_INSUFFICIENT);

        assertThat(success.code()).isZero();
        assertThat(success.message()).isEqualTo("success");
        assertThat(success.data()).isEqualTo("order-id");
        assertThat(failure.code()).isEqualTo(40001);
        assertThat(failure.message()).isEqualTo("库存不足");
        assertThat(failure.data()).isNull();
    }
}
