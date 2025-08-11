package com.koo.bonscore.core.config.web.security.config;

import com.koo.bonscore.core.config.web.security.filter.JwtAuthenticationFilter;
import com.koo.bonscore.core.config.web.security.filter.RedirectValidationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity // ✅ NOTE: 오토와이어링할 수 없습니다. 'HttpSecurity' 타입의 bean을 찾을 수 없습니다. 해결 방법
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final LoginSessionManager loginSessionManager;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins; // 운영환경별 허용 url 분리

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // CORS 설정
                .csrf(AbstractHttpConfigurer::disable)  // REST API를 위한 CSRF 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 사용 시 세션 비활성화
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, loginSessionManager), UsernamePasswordAuthenticationFilter.class) // JWT 인증 필터 추가
                .addFilterBefore(new RedirectValidationFilter(), JwtAuthenticationFilter.class) // 검증되지 않은 리다이렉트 및 포워드 방어
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()  // 로그인 API는 인증 없이 허용
                        .requestMatchers("/api/public-key/**").permitAll()
                        .anyRequest().authenticated()                   // 나머지는 인증 필요
                )
                .build();
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 설정을 위한 Bean.
     * 다른 도메인(Origin)의 프론트엔드 애플리케이션이 이 서버의 API에 접근할 수 있도록 허용하는 정책을 정의
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));        // 허용할 프론트엔드 주소
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));    // 허용할 HTTP 메서드
        configuration.setAllowedHeaders(List.of("*"));                                      // 모든 요청 헤더 허용
        configuration.setAllowCredentials(true);                                                // 인증 정보 포함 허용 (JWT 사용 시 필요)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Spring Security의 자동 유저 생성을 막기 위한 용도
     * - 혹시라도 어떤 경로로 이 Bean이 호출되더라도 예외를 발생시키므로 의도치 않은 동작을 방지
     * @return
     */
    @Bean
    public UserDetailsService userDetailService() {
        return username -> {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
        };
    }
}
