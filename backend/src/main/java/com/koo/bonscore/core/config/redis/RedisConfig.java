package com.koo.bonscore.core.config.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <pre>
 * RedisConfig.java
 * 설명 : Redis 설정파일
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-07-24
 */
@Configuration
public class RedisConfig {

    // application-env.yml의 경로에서 값을 읽어옵니다.
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    /**
     * Redis 서버와의 연결을 담당하는 ConnectionFactory Bean을 생성합니다.
     * Lettuce는 Netty 기반의 고성능 Redis 클라이언트입니다.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    /**
     * 실제 서비스에서 Redis에 쉽게 접근하기 위한 StringRedisTemplate Bean을 생성합니다.
     * 이 Bean이 다른 서비스 클래스(@Service)에 주입되어 사용됩니다.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(redisConnectionFactory());
        return stringRedisTemplate;
    }
}
