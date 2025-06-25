package com.koo.bonscore.core.aop;

import com.koo.bonscore.core.annotaion.PreventDoubleClick;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 * PreventDoubleClickAspect.java
 * 설명 : 중복 클릭(요청) 방지용 AOP (로그인 등 익명 사용자 지원)
 * </pre>
 * @author  : koobonsang
 * @version : 1.1
 * @since   : 2025-01-14
 */
@Slf4j
@Aspect
@Component
public class PreventDoubleClickAspect {

    private final ConcurrentHashMap<String, Long> processingRequests = new ConcurrentHashMap<>();

    @Around("@annotation(com.koo.bonscore.core.annotaion.PreventDoubleClick)")
    public Object preventDoubleClick(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        PreventDoubleClick annotation = method.getAnnotation(PreventDoubleClick.class);

        // 파라미터를 제외하고 키 생성
        String requestKey = createRequestKeyWithoutParams(joinPoint);

        if (requestKey == null) {
            log.warn("중복 방지 키 생성에 실패하여 AOP를 건너뜁니다. Method: {}", signature.getName());
            return joinPoint.proceed();
        }

        long currentTime = System.currentTimeMillis();
        Long lastRequestTime = processingRequests.putIfAbsent(requestKey, currentTime);

        if (lastRequestTime != null) {
            long timeElapsed = currentTime - lastRequestTime;
            long timeoutMillis = annotation.timeUnit().toMillis(annotation.timeout());

            if (timeElapsed < timeoutMillis) {
                log.warn("중복 요청이 감지되었습니다. [Key: {}]", requestKey);
                throw new BsCoreException(HttpStatusCode.INTERNAL_SERVER_ERROR, ErrorCode.PREVENT_DOUBLE_REQUEST);

            } else {
                processingRequests.put(requestKey, currentTime);
            }
        }

        log.info("요청 처리 시작. [Key: {}]", requestKey);

        try {
            return joinPoint.proceed();
        } finally {
            processingRequests.remove(requestKey);
            log.info("요청 처리 완료. 키 제거. [Key: {}]", requestKey);
        }
    }

    /**
     * **파라미터를 제외하고** 요청을 고유하게 식별하기 위한 키를 생성합니다.
     * 암호화된 값 등 매번 바뀌는 파라미터가 있는 경우에 이 방법을 사용합니다.
     * 키 조합: [사용자 식별자]:[클래스명].[메서드명]
     *
     * @param joinPoint the join point
     * @return a unique key for the request, or null if context is not available
     */
    private String createRequestKeyWithoutParams(ProceedingJoinPoint joinPoint) {
        String userIdentifier = getUserIdentifier();
        if (userIdentifier == null) {
            return null;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        String resultKey = String.join(":", userIdentifier, className, methodName);
        log.info("### 생성된 중복 방지 키 (파라미터 제외): [{}]", resultKey);
        return resultKey;
    }

    private String getUserIdentifier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        } else {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return getClientIp(attributes.getRequest());
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
