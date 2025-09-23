package com.koo.bonscore.core.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <pre>
 * LoggingAspect.java
 * 설명 : 로깅용 Aop
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-01-13
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * @Pointcut: @Service 애노테이션이 붙은 클래스 내부의 모든 메서드에 대해 포인트컷 설정
     */
    @Pointcut("within(@org.springframework.stereotype.Service *) && !execution(* com.koo.bonscore.common.file.service.FileStorageService.storeFile(..))")
    public void serviceMethods() {}

    /**
     * @Before: 서비스 메서드 실행 전에 로깅
     *
     * @param joinPoint
     * @throws JsonProcessingException
     */
    @Before("serviceMethods()")
    public void logBefore(JoinPoint joinPoint) throws JsonProcessingException {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();

        ObjectMapper objectMapper = new ObjectMapper();
        String params = objectMapper.writeValueAsString(joinPoint.getArgs());

        String sb = "\n" +
                "+------------------------------------------------------------------+\n" +
                "|                          SERVICE START                           |\n" +
                "+------------------------------------------------------------------+\n" +
                "| Class      : " + sig.getDeclaringTypeName() + "\n" +
                "| Method     : " + sig.getName() + "\n" +
                "| Parameters : " + params + "\n" +
                "| Start Time : " + getDateTime() + "\n" +
                "+------------------------------------------------------------------+";
        log.info(sb);
    }

    /**
     * @AfterReturning: 서비스 메서드가 정상적으로 실행 완료되었을 때 로깅
     *
     * @param joinPoint
     * @param result
     */
    @AfterReturning(pointcut = "serviceMethods()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();

        String sb = "\n" +
                "+------------------------------------------------------------------+\n" +
                "|                           SERVICE END                            |\n" +
                "+------------------------------------------------------------------+\n" +
                "| Class      : " + sig.getDeclaringTypeName() + "\n" +
                "| Method     : " + sig.getName() + "\n" +
                "| Result     : " + result + "\n" +
                "| End Time   : " + getDateTime() + "\n" +
                "+------------------------------------------------------------------+";
        log.info(sb);
    }

    /**
     * @AfterThrowing: 서비스 메서드 실행 중 예외가 발생했을 때 로깅
     *
     * @param joinPoint
     * @param ex
     */
    @AfterThrowing(pointcut = "serviceMethods()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();

        String sb = "\n" +
                "+------------------------------------------------------------------+\n" +
                "|                          EXCEPTION !!                            |\n" +
                "+------------------------------------------------------------------+\n" +
                "| Class            : " + sig.getDeclaringTypeName() + "\n" +
                "| Method           : " + sig.getName() + "\n" +
                "| Exception Time   : " + getDateTime() + "\n" +
                "| Ex               : " + ex.getMessage() + "\n"
                                        + ex + "\n" +
                "+------------------------------------------------------------------+";
        log.error(sb);
    }

    /**
     * 현재시간을 반환한다
     * @return yyyy-MM-dd HH:mm:ss
     */
    public String getDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }

}
