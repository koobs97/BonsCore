package com.koo.bonscore.core.config.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * <pre>
 * DatabaseConnectionChecker.java
 * 설명 : 어플리케이션 구동 시 db 연결 확인 컴포넌트
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-13
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseConnectionChecker implements CommandLineRunner {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    private static final String RESET = "\u001B[0m";
    private static final String GREEN_BOLD = "\u001B[32;1m";
    private static final String RED_BOLD = "\u001B[31;1m";
    private static final String CYAN = "\u001B[36m";

    @Override
    public void run(String... args) {
        checkDatabaseConnection();
        checkRedisConnection();
    }

    /**
     * DB 연결 확인용
     */
    private void checkDatabaseConnection() {
        try (Connection conn = dataSource.getConnection()) {
            String dbUrl = conn.getMetaData().getURL();
            // URL이 너무 길면 줄임 처리 (가독성을 위해)
            if (dbUrl.length() > 50) dbUrl = dbUrl.substring(0, 47) + "...";

            String productName = conn.getMetaData().getDatabaseProductName();
            String version = conn.getMetaData().getDatabaseProductVersion().split("\n")[0]; // 첫 줄만

            log.info("DB Connection    : [ {}OK{} ] {} ({}) - {}",
                    GREEN_BOLD, RESET, productName, version, dbUrl);

        } catch (Exception e) {
            log.error("DB Connection    : [ {}FAIL{} ] Reason: {}", RED_BOLD, RESET, e.getMessage());
        }
    }

    /**
     * Redis 연결 확인용
     */
    private void checkRedisConnection() {
        try {
            RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
            if (factory == null) throw new RuntimeException("RedisFactory is null");

            try (RedisConnection connection = factory.getConnection()) {
                String response = connection.ping();

                String hostInfo = "Unknown";
                if (factory instanceof LettuceConnectionFactory lettuce) {
                    hostInfo = lettuce.getHostName() + ":" + lettuce.getPort();
                }

                if ("PONG".equalsIgnoreCase(response)) {
                    log.info("Redis Connection : [ {}OK{} ] Host: {}", GREEN_BOLD, RESET, hostInfo);
                } else {
                    throw new RuntimeException("Invalid PING response: " + response);
                }
            }
        } catch (Exception e) {
            log.error("Redis Connection : [ {}FAIL{} ] Reason: {}", RED_BOLD, RESET, e.getMessage());
        }
    }
}
