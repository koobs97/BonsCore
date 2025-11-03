package com.koo.bonscore.core.aop;

import com.koo.bonscore.core.config.web.security.util.CommandInjectionPreventUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CommandExecutionAspect {

    @Pointcut("@annotation(com.koo.bonscore.core.annotaion.PreventCommandInjection)")
    public void commandValidationRequired() {}

    @Before("commandValidationRequired()")
    public void validateCommandArgs(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof String input) {
                if (!CommandInjectionPreventUtil.isSafeInput(input)) {
                    throw new IllegalArgumentException("Unsafe command input detected: " + input);
                }
            }
        }
    }

}
