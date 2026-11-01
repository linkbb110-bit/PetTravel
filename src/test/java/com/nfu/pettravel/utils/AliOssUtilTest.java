package com.nfu.pettravel.utils;

import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AliOssUtilTest {

    @Test
    void shouldRejectExecutableBeforeContactingOss() {
        AliOssUtil util = new AliOssUtil(
                "oss-cn-guangzhou.aliyuncs.com", "id", "secret", "bucket", "", 10_000_000);
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.exe", "application/octet-stream", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> util.upload(file))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_INVALID));
    }
}
