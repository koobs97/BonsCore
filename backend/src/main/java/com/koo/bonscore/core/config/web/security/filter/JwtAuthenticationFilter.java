package com.koo.bonscore.core.config.web.security.filter;

import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.config.web.security.config.LoginSessionManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Order(1)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final LoginSessionManager loginSessionManager;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = jwtTokenProvider.resolveToken(request);

        // 토큰 유효성 검사 전 블랙리스트 확인
        if (token != null) {

            // 1. 블랙리스트에 토큰이 있는지 확인
            if (loginSessionManager.isTokenBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("This token has been invalidated by a newer login.");
                return;
            }

            // 2. 토큰 유효성 검증
            if (jwtTokenProvider.validateToken(token)) {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                // 만료되거나 잘못된 토큰인 경우
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired token.");
                return;
            }

        }

        filterChain.doFilter(request, response);
    }
}
