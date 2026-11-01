package com.nfu.pettravel.common.aspect;


import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.enums.UserRole;
import com.nfu.pettravel.utils.UserContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import com.nfu.pettravel.exception.BusinessException;
@Aspect
@Component
public class AdminOnlyAspect {

    @Before("@annotation(com.nfu.pettravel.common.annotation.AdminOnly) || "
            + "@within(com.nfu.pettravel.common.annotation.AdminOnly)")
    public void assertAdmin() {
        if (UserContext.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (UserContext.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
