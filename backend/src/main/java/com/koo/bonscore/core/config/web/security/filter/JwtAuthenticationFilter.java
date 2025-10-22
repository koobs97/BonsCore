package com.koo.bonscore.core.config.web.security.filter;
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
import org.springframework.beans.factory.annotation.Qualifier;
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

@Slf4j
@Component
@Order(1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final LoginSessionManager loginSessionManager;
    private final HandlerExceptionResolver handlerExceptionResolver;

    // 생성자 주입
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   LoginSessionManager loginSessionManager,
                                   @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginSessionManager = loginSessionManager;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
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
