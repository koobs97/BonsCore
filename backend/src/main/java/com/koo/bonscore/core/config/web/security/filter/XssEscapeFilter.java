package com.koo.bonscore.core.config.web.security.filter;

import com.koo.bonscore.core.config.web.security.XssEscapeServletRequestWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * <pre>
 * XssEscapeFilter.java
 * 설명 : 모든 HTTP 요청을 가로채서, 요청 객체를 XssEscapeServletRequestWrapper로 감싸는 서블릿 필터
 *
 *       이 필터를 통해 클라이언트에서 전달된 파라미터와 헤더값에 포함된
 *       잠재적인 XSS 공격 코드를 HTML 이스케이프 처리하여 보안 취약점을 완화
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-06-03
 */
public class XssEscapeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 원본 HttpServletRequest를 XssEscapeServletRequestWrapper로 감싸서 넘김
        HttpServletRequest req = (HttpServletRequest) request;

        // 래핑된 요청 객체를 다음 필터나 컨트롤러로 전달
        chain.doFilter(new XssEscapeServletRequestWrapper(req), response);
    }
}
