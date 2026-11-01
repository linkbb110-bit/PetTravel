package com.nfu.pettravel.common.aspect;

import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.enums.UserRole;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminOnlyAspectTest {

    @AfterEach
    void cleanContext() {
        UserContext.removeUserId();
    }

    @Test
    void shouldRejectNormalUserAndAllowAdmin() {
        AdminOnlyAspect aspect = new AdminOnlyAspect();
        UserContext.setUser(1L, UserRole.USER);

        assertThatThrownBy(aspect::assertAdmin)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        UserContext.setUser(2L, UserRole.ADMIN);
        aspect.assertAdmin();
    }

    @Test
    void shouldReportUnauthorizedWhenNoAuthenticatedUserExists() {
        AdminOnlyAspect aspect = new AdminOnlyAspect();

        assertThatThrownBy(aspect::assertAdmin)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}
