package com.koo.bonscore.core.config.web.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * <pre>
 * PasswordEncoderConfig.java
 * 설명 : 비밀번호 암호화 객체(PasswordEncoder)를 빈으로 등록하는 전용 설정 클래스
 *
 * [분리 사유]
 * 기존 SecurityConfig 내부에서 PasswordEncoder를 정의할 경우,
 * SecurityConfig 초기화 시점과 AuthService 등의 비즈니스 로직 빈 생성 시점이 얽혀
 * "순환 참조(Circular Dependency)" 에러가 발생할 수 있다.
 *
 * (예: SecurityConfig -> CustomOAuth2UserService -> AuthService -> PasswordEncoder -> SecurityConfig)
 *
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-12-04
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCryptPasswordEncoder 빈 등록
     *
     * <pre>
     * 설명 :
     * - Spring Security에서 표준으로 권장하는 BCrypt 해시 알고리즘을 사용.
     * - 내부적으로 Random Salt를 포함하여 해싱하므로, 같은 비밀번호라도 DB에는 매번 다른 값으로 저장된다.
     * </pre>
     *
     * @return BCryptPasswordEncoder 객체
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
