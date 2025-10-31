package com.example.toycontent.app.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
    boolean required() default true;  // 기본값은 필수
}

