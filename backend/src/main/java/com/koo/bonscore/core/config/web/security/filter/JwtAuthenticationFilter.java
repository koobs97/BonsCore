package com.koo.bonscore.core.config.web.security.filter;

import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.config.web.security.config.LoginSessionManager;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.rmi.server.ExportException;

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
                    Authentication auth = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    // 만료되거나 잘못된 토큰인 경우
                    throw new ExportException("잘못된 인증정보입니다.");
                }

            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
        }

    }
}
