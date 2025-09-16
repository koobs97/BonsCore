package com.koo.bonscore.core.config.web.security.config;

import com.koo.bonscore.core.config.web.security.filter.JwtAuthenticationFilter;
import com.koo.bonscore.core.config.web.security.filter.RedirectValidationFilter;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.Arrays;
import java.util.List;

/**
 * <pre>
 * SecurityConfig.java
 * 설명 : 애플리케이션의 주된 웹 보안 설정을 담당하는 클래스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-03-25
 */
@Configuration
@EnableWebSecurity // 오토와이어링할 수 없습니다. 'HttpSecurity' 타입의 bean을 찾을 수 없습니다. 해결
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final LoginSessionManager loginSessionManager;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 생성자
     * @param jwtTokenProvider
     * @param loginSessionManager
     * @param handlerExceptionResolver
     * @param jwtAuthenticationFilter
     */
    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          LoginSessionManager loginSessionManager,
                          @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver, // <-- 여기에 직접 @Qualifier 추가
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginSessionManager = loginSessionManager;
        this.handlerExceptionResolver = handlerExceptionResolver;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 운영환경별 허용 url 분리
     */
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * 비밀번호 암호화를 위한 BCryptPasswordEncoder를 Bean으로 등록.
     * Spring Security에서 권장하는 강력한 해시 알고리즘 중 하나이다.
     *
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security의 핵심 필터 체인을 구성하고 Bean으로 등록
     * HTTP 요청에 대한 보안 처리가 이 필터 체인을 통해 이루어진다.
     *
     * @param http HttpSecurity 객체. 보안 관련 설정을 구성하는 빌더
     * @return 구성이 완료된 SecurityFilterChain 객체
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // CORS 설정
                .csrf(AbstractHttpConfigurer::disable)  // REST API를 위한 CSRF 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 사용 시 세션 비활성화
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // JWT 인증 필터 추가
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
     * @return UserDetailsService의 익명 구현체
     */
    @Bean
    public UserDetailsService userDetailService() {
        return username -> {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
        };
    }
}
