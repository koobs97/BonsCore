package com.koo.bonscore.core.config.web.security.filter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.config.web.security.config.LoginSessionManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <pre>
 * JwtAuthenticationFilter.java
 * 설명 : HTTP 요청의 헤더에서 JWT(JSON Web Token)를 파싱하여 인증을 처리하는 Spring Security 필터
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-01
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final LoginSessionManager loginSessionManager;
    private final HandlerExceptionResolver handlerExceptionResolver;

    /**
     * 모든 HTTP 요청에 대해 JWT 인증을 시도하는 필터 메소드
     *
     * @param request       HTTP 서블릿 요청 객체
     * @param response      HTTP 서블릿 응답 객체
     * @param filterChain   다음 필터를 호출하기 위한 필터 체인 객체
     * @throws ServletException 서블릿 관련 예외 발생 시
     * @throws IOException      입출력 관련 예외 발생 시
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // 요청 헤더에서 JWT 토큰을 추출
            String token = jwtTokenProvider.resolveToken(request);

            // 토큰 유효성 검사 전 블랙리스트 확인
            if (token != null) {

                // 1. 블랙리스트에 토큰이 있는지 확인
                if (loginSessionManager.isTokenBlacklisted(token)) {
                    throw new JwtException("중복 로그인이 감지되어 강제 로그아웃 됩니다.");
                }

                // 2. 토큰 유효성 검증
                if (jwtTokenProvider.validateToken(token)) {
                    Authentication auth = this.getAuthenticationWithRoles(token);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    // 만료되거나 잘못된 토큰인 경우
                    throw new JwtException("세션이 만료되었습니다.");
                }

            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
        }

    }

    /**
     * 유효한 JWT에서 사용자 정보와 권한을 추출하여 Spring Security가 이해할 수 있는
     * {@link Authentication} 객체를 생성
     *
     * @param token 유효성이 검증된 JWT 문자열
     * @return 사용자 ID(Principal), 자격 증명(Credentials), 권한(Authorities)을 포함하는
     *          {@link UsernamePasswordAuthenticationToken} 객체
     */
    private Authentication getAuthenticationWithRoles(String token) {
        Claims claims = jwtTokenProvider.getClaims(token);
        List<String> roles = claims.get("roles", List.class);
        Collection<? extends GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        String userId = claims.getSubject();

        // 비밀번호는 이미 인증되었으므로 빈 문자열(""), 혹은 "PROTECTED" 등으로 채웁니다.
        UserDetails principal = new User(userId, "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }
}