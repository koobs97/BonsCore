package com.koo.bonscore.common.util.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * <pre>
 * WebUtils.java
 * 설명 : 웹 환경과 관련된 유틸리티 메소드를 제공하는 클래스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-10-28
 */
public class WebUtils {

    /**
     * WebUtils 클래스는 인스턴스화할 수 없다.
     */
    private WebUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 프록시 서버(Nginx, L4 스위치 등) 환경에서도 실제 클라이언트의 IP 주소를 얻기 위한 헬퍼 메소드.
     * <p>
     * 표준/비표준 HTTP 헤더들을 순차적으로 확인하고, 최종적으로는 {@code request.getRemoteAddr()}를 호출.
     * </p>
     *
     * @param request HTTP 요청 객체.
     * @return 추출된 클라이언트의 IP 주소.
     */
    public static String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) ip = request.getHeader("Proxy-Client-IP");
        if (ip == null) ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip == null) ip = request.getHeader("HTTP_CLIENT_IP");
        if (ip == null) ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip == null) ip = request.getRemoteAddr();
        return ip;
    }
}
