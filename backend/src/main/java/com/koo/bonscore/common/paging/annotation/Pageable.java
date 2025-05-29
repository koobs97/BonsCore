package com.koo.bonscore.common.paging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 * Pageable.java
 * 설명 : 페이징 처리를 위한 메서드용 커스텀 애노테이션
 *       공통 페이징 처리 로직 적용 시 사용됨
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-28
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pageable {
}
