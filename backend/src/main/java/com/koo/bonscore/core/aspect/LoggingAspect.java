package com.koo.bonscore.core.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {}

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
                "+------------------------------------------------------------------+";
        log.info(sb);
    }

    @AfterReturning(pointcut = "serviceMethods()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        log.info("⏹ END: {}.{} => return={}",
                sig.getDeclaringTypeName(),
                sig.getName(),
                result);
    }

    @AfterThrowing(pointcut = "serviceMethods()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        log.error("💥 EXCEPTION: {}.{} => ex={}",
                sig.getDeclaringTypeName(),
                sig.getName(),
                ex.getMessage(), ex);
    }

}
