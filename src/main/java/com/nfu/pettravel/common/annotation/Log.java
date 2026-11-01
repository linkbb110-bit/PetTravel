package com.nfu.pettravel.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 作用于方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时有效
public @interface Log {
    // 我们可以给注解加个属性，用来描述这个操作是干嘛的
    String title() default "";
}