package com.example.toycontent.app.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 현재 인증된 사용자가 ADMIN 권한을 가지고 있는지 boolean 으로 주입한다.
 * 인증되지 않았거나 ADMIN 이 아니면 false.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserIsAdmin {
}
