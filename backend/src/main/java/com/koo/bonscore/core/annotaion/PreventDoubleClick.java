package com.koo.bonscore.core.annotaion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD) // 메서드에만 적용 가능
@Retention(RetentionPolicy.RUNTIME) // 런타임까지 어노테이션 정보 유지
public @interface PreventDoubleClick {

    /**
     * 중복 요청을 막을 시간 (기본값: 3초)
     * 이 시간 내에 들어온 동일한 요청은 무시됩니다.
     */
    long timeout() default 3L;

    /**
     * 시간 단위 (기본값: 초)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}