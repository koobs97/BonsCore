package com.koo.bonscore.core.config.mybatis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
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

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                String sb = "\n═══════════════════════════════════════════════════════════════\n" +
                        "✅ DB 연결 성공!\n" +
                        "📌 DB URL: " + conn.getMetaData().getURL() + "\n" +
                        "📌 DB 버전: " + conn.getMetaData().getDatabaseProductVersion() + "\n" +
                        "═══════════════════════════════════════════════════════════════";

                log.info(sb);
            }
        } catch (Exception e) {
            System.err.println("❌ DB 연결 실패: " + e.getMessage());
            e.fillInStackTrace();
        }
    }
}
