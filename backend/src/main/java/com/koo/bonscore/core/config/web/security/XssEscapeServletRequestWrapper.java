package com.koo.bonscore.core.config.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * <pre>
 * XssEscapeServletRequestWrapper.java
 * 설명 : HttpServletRequestWrapper를 상속하여 HTTP 요청의 파라미터와 헤더 값을
 *       XSS 공격에 취약한 HTML 특수 문자를 이스케이프 처리하는 커스텀 요청 래퍼
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-06-03
 */
public class XssEscapeServletRequestWrapper extends HttpServletRequestWrapper {

    public XssEscapeServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * 요청 파라미터 단일 값에 대해 XSS 방어용 이스케이프 처리 수행
     */
    @Override
    public String getParameter(String name) {
        return sanitize(super.getParameter(name));
    }

    /**
     * 요청 파라미터 다중 값에 대해 각각 이스케이프 처리 수행
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;

        String[] sanitizedValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            sanitizedValues[i] = sanitize(values[i]);
        }
        return sanitizedValues;
    }

    /**
     * 요청 헤더 값에 대해서도 XSS 방어용 이스케이프 처리 수행
     */
    @Override
    public String getHeader(String name) {
        return sanitize(super.getHeader(name));
    }

    /**
     * 실제 이스케이프 처리 메서드
     * 특수 문자를 HTML 엔티티로 변환하여 스크립트 실행 방지
     */
    private String sanitize(String input) {
        if (input == null) return null;

        // & → &amp; (먼저 변환해야 중복 변환 방지)
        // < → &lt;
        // > → &gt;
        // " → &quot;
        // ' → &#x27;
        // / → &#x2F;
        return input
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;")
                .replaceAll("/", "&#x2F;");
    }
}
