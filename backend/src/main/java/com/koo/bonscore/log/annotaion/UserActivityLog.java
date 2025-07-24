package com.koo.bonscore.log.annotaion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 * UserActivityLog.java
 * 설명 : 사용자 활동 로그 기록을 트리거하는 커스텀 어노테이션
 * </pre>
 *
 * <p>
 * 이 어노테이션이 부착된 컨트롤러 메소드가 호출될 때,
 * {@link com.koo.bonscore.log.aop.UserActivityLogAspect}에 의해 관련 정보가 가로채져
 * {@code HttpServletRequest}에 속성으로 추가된다.
 * 이 정보는 추후 {@link com.koo.bonscore.log.interceptor.UserActivityLogInterceptor}에서
 * 최종 로그를 생성하는 데 사용된다.
 * </p>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-07-23
 *
 * @see com.koo.bonscore.log.aop.UserActivityLogAspect
 * @see com.koo.bonscore.log.interceptor.UserActivityLogInterceptor
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserActivityLog {

    /**
     * 기록될 활동의 유형을 지정.
     * <p>
     * 이 값은 {@code USER_ACTIVITY_LOG} 테이블의 {@code ACTIVITY_TYPE} 컬럼에 저장된다.
     * 예: "LOGIN", "SIGNUP", "LOGOUT", "UPDATE_MY_INFO"
     * </p>
     *
     * @return 활동 유형 문자열
     */
    String activityType();

    /**
     * 사용자 ID를 추출하기 위한 SpEL(Spring Expression Language) 표현식을 지정한다.
     * <p>
     * 이 필드는 주로 로그인, 회원가입과 같이 **인증되지 않은 상태**에서
     * 사용자가 입력한 ID를 로그에 기록해야 할 때 사용된다.
     * 표현식의 루트 컨텍스트는 해당 메소드의 파라미터들이다.
     * </p>
     * <p>
     * 이 필드를 비워두거나 `null`로 둘 경우, AOP는 {@code SecurityContextHolder}에서
     * 현재 **인증된 사용자**의 ID를 가져오려고 시도한다.
     * </p>

     * @return 사용자 ID를 추출하기 위한 SpEL 표현식.
     */
    String userIdField() default ""; // SpEL 표현식. 비워두면 SecurityContext에서 찾도록 할 예정
}