package com.koo.bonscore.common.masking.filter;

import com.koo.bonscore.common.masking.context.MaskingContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <pre>
 * MaskingClearFilter.java
 * 설명 : 요청 처리 후 ThreadLocal에 저장된 마스킹 설정(MaskingContext)을 제거하는 필터 클래스.
 *       마스킹 설정은 서비스 계층에서 동적으로 제어되며, 해당 설정은 직렬화(Jackson) 타이밍까지 유지되어야 함.
 *       이 필터는 모든 HTTP 요청에 대해 한 번만 실행되며, 응답 직전에 MaskingContext를 정리하여 메모리 누수 및 설정 유지 문제를 방지함.
 *       반드시 OncePerRequestFilter를 상속하여 요청당 한 번만 실행되도록 보장해야 함.
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-06-03
 */
@Component
public class MaskingClearFilter extends OncePerRequestFilter {

    /**
     * HTTP 요청을 처리하고, 응답 직전에 MaskingContext를 정리함.
     *
     * @param request      HTTP 요청 객체
     * @param response     HTTP 응답 객체
     * @param filterChain  필터 체인
     * @throws ServletException 필터 처리 중 예외 발생 시
     * @throws IOException      입출력 예외 발생 시
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 다음 필터 또는 실제 컨트롤러로 요청을 전달
            filterChain.doFilter(request, response);
        } finally {
            // 요청 처리가 완료된 후 반드시 마스킹 설정 정리 (ThreadLocal 해제)
            MaskingContext.clear();
        }
    }
}
