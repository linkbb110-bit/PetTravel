package com.nfu.pettravel.common.aspect;

import com.nfu.pettravel.common.annotation.Log;
import com.nfu.pettravel.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect // 告诉 Spring 这是一个切面类
@Component // 交给 Spring 容器管理
@Slf4j // Lombok 提供的日志注解，直接用 log.info()
public class LogAspect {

    // @Around 环绕通知：可以在目标方法执行前后都做事情（比如计算耗时）
    @Around("@annotation(logAnnotation)") // 拦截所有加了 @Log 的方法
    public Object around(ProceedingJoinPoint point, Log logAnnotation) throws Throwable {
        long beginTime = System.currentTimeMillis();
        boolean succeeded = false;
        try {
            Object result = point.proceed();
            succeeded = true;
            return result;
        } finally {
            long time = System.currentTimeMillis() - beginTime;
            Long userId = UserContext.getUserId();
            log.info("【操作日志】操作人ID: {}, 模块: {}, 方法: {}, 成功: {}, 耗时: {}ms",
                    userId != null ? userId : "匿名",
                    logAnnotation.title(),
                    point.getSignature().getName(),
                    succeeded,
                    time);
        }
    }
}
