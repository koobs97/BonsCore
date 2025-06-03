package com.koo.bonscore.core.aspect;

import com.koo.bonscore.core.config.web.security.util.CommandInjectionPreventUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CommandExecutionAspect {

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {}

    @Before("serviceMethods()")
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
