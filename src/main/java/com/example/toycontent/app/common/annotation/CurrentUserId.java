package com.example.toycontent.app.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {
    /**
     * JWT 클레임 키 이름 (기본값: "userId")
     */
    String value() default "userId";
}

