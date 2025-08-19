package com.koo.bonscore;

import com.koo.bonscore.common.api.naver.NaverProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BonsCoreApplication
 * sdf
 *
 */
@EnableConfigurationProperties(NaverProperties.class) // naver api
@EnableAsync // 로깅을 위한 비동기처리 활성화
@EnableScheduling
@SpringBootApplication
public class BonsCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BonsCoreApplication.class, args);
    }

}
