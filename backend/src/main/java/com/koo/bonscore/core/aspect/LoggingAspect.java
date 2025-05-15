package com.koo.bonscore.core.aspect;

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
    public void logBefore(JoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        log.info("▶ START: {}.{}({})",
                sig.getDeclaringTypeName(),
                sig.getName(),
                Arrays.toString(joinPoint.getArgs()));
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
