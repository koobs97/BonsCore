package com.koo.bonscore.log.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * <pre>
 * AsyncConfig.java
 * 설명 : 스레드 풀 설정
 *
 * 기본적으로 Spring은 SimpleAsyncTaskExecutor를 사용하는데, 이는 매 요청마다 새로운 스레드를 생성하여 성능 저하 유발
 * ThreadPoolTaskExecutor를 사용하여 스레드 풀을 직접 설정하고 관리
 *
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-07-23
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "logTaskExecutor")
    public Executor logTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      // 기본 스레드 수
        executor.setMaxPoolSize(30);      // 최대 스레드 수
        executor.setQueueCapacity(100);   // 큐 사이즈
        executor.setThreadNamePrefix("Log-Executor-");
        executor.initialize();
        return executor;
    }
}