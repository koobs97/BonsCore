package com.koo.bonscore;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <pre>
 * BonsCoreApplication.java
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-01-07
 */
@EnableCaching
@EnableAsync // 로깅을 위한 비동기처리 활성화
@EnableScheduling
@SpringBootApplication
public class BonsCoreApplication {

    public static void main(String[] args) {

        // .env 적용
        Dotenv dotenv = Dotenv.configure()
                .directory("./backend")
                .systemProperties()
                .load();

        SpringApplication.run(BonsCoreApplication.class, args);
    }

}
