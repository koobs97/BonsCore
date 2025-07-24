package com.koo.bonscore.log.aop;

import com.koo.bonscore.log.annotaion.UserActivityLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <pre>
 * UserActivityLogAspect.java
 * 설명 : {@link UserActivityLog} 어노테이션을 처리하는 AOP Aspect
 * </pre>
 *
 * <p>
 * 이 Aspect는 {@code @UserActivityLog}가 붙은 메소드가 실행되기 전({@code @Before})에 동작하여,
 * 로깅에 필요한 주요 정보(활동 유형, 사용자 ID)를 추출하고 이를 {@code HttpServletRequest}의
 * 속성(attribute)으로 설정한다. 이렇게 설정된 정보는 요청 처리 파이프라인의 후반부에서
 * {@code UserActivityLogInterceptor}에 의해 사용되어 최종 로그를 생성하게 된다.
 * </p>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-07-23
 */
@Aspect
@Component
@Slf4j
public class UserActivityLogAspect {

    /**
     *
     * {@code @UserActivityLog} 어노테이션이 지정된 메소드가 실행되기 전에 호출되는 Advice이다.
     * <p>
     * 이 메소드는 어노테이션에 정의된 정보를 바탕으로 활동 유형(activityType)과 사용자 ID(userId)를
     * 결정하고, 이를 {@code HttpServletRequest}에 각각 'activityType'과 'userId'라는 이름의
     * 속성으로 저장한다.
     * </p>
     *
     * @param joinPoint
     * @param userActivityLog
     */
    @Before("@annotation(userActivityLog)")
    public void setLogAttributes(JoinPoint joinPoint, UserActivityLog userActivityLog) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("HttpServletRequest를 찾을 수 없어 로그 속성 설정을 건너뜁니다.");
            return;
        }
        HttpServletRequest request = attributes.getRequest();

        // 1. 어노테이션에서 activityType을 가져와 request 속성에 설정
        request.setAttribute("activityType", userActivityLog.activityType());

        // 2. 사용자 ID를 결정한다. 우선순위는 SpEL > SecurityContext
        String userId = null;
        String userIdFieldExpression = userActivityLog.userIdField();

        // 2-1. userIdField에 SpEL 표현식이 지정된 경우, 메소드 파라미터에서 먼저 userId를 추출한다.
        //      주로 로그인, 회원가입과 같이 인증되지 않은 사용자의 활동에 사용된다.
        if (StringUtils.hasText(userIdFieldExpression)) {
            userId = getUserIdFromRequest(joinPoint, userIdFieldExpression);
        }

        // 2-2. SpEL로 userId를 찾지 못했거나, 애초에 표현식이 지정되지 않은 경우,
        //      SecurityContext에서 현재 인증된 사용자의 정보를 찾는다.
        //      주로 로그아웃, 정보 수정과 같이 인증된 사용자의 활동에 사용된다.
        if (!StringUtils.hasText(userId)) {
            userId = getUserIdFromSecurityContext();
        }

        if (userId != null) {
            request.setAttribute("userId", userId);
        }
    }

    /**
     * SpEL(Spring Expression Language)을 사용하여 메소드 파라미터에서 동적으로 사용자 ID를 추출
     *
     * @param joinPoint
     * @param userIdField
     * @return
     */
    private String getUserIdFromRequest(JoinPoint joinPoint, String userIdField) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            ExpressionParser parser = new SpelExpressionParser();
            EvaluationContext context = new StandardEvaluationContext();

            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }

            return parser.parseExpression(userIdField).getValue(context, String.class);
        } catch (Exception e) {
            log.error("SpEL을 통해 userId를 추출하는 데 실패했습니다. expression: {}", userIdField, e);
            return null;
        }
    }

    /**
     * {@code SecurityContextHolder}에서 현재 인증된 사용자의 ID(Principal)를 가져온다.
     *
     * @return 사용자 ID, 인증 정보가 없으면 null
     */
    private String getUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            // Spring Security의 UserDetails를 사용하는 경우
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            // JWT 토큰 등에서 사용자 ID(String)를 바로 Principal로 설정한 경우
            String userId = (String) principal;
            // "anonymousUser"는 인증되지 않은 사용자를 의미하므로 제외
            return "anonymousUser".equals(userId) ? null : userId;
        }

        return null;
    }
}