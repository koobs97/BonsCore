package com.koo.bonscore.core.config.web.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <pre>
 * RedirectValidationFilter.java
 * 설명 : 검증되지 않은 리다이렉트(redirect) 파라미터를 방지하기 위한 필터
 *       외부 URL 리디렉션 방지
 *       URI 형식(`/dashboard`, `/home/main` 등)만 허용
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-06-03
 */
@Order(2)
public class RedirectValidationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String redirectParam = request.getParameter("redirect");

        if (redirectParam != null) {
            // ✅ /로 시작하고 알파벳/숫자/-/_/만 허용
            boolean isSafe = redirectParam.matches("^/([a-zA-Z0-9\\-_/]*)$");
            if (!isSafe) {
                // 로그 찍기 or 에러 응답
                // 서블릿 수준에서의 오류로 에러핸들러로 가지 않고 처리됨
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid redirect URL");
                return;
            }
        }

        // 문제가 없으면 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}
