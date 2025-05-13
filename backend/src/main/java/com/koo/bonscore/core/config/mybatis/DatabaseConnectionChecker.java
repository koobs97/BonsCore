package com.koo.bonscore.core.config.mybatis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DatabaseConnectionChecker implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                String sb = "\n═══════════════════════════════════════════════════════════════\n" +
                        "✅ DB 연결 성공!\n" +
                        "📌 DB URL: " + conn.getMetaData().getURL() + "\n" +
                        "📌 DB 버전: " + conn.getMetaData().getDatabaseProductVersion() + "\n" +
                        "═══════════════════════════════════════════════════════════════\n";

                System.out.println(sb);
            }
        } catch (Exception e) {
            System.err.println("❌ DB 연결 실패: " + e.getMessage());
            e.fillInStackTrace();
        }
    }
}
