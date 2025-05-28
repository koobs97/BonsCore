package com.koo.bonscore.common.paging.interceptor;

import com.koo.bonscore.common.paging.PageContext;
import com.koo.bonscore.core.config.web.WebConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * <pre>
 * PageClearInterceptor.java
 * 설명 : ThreadLocal 정리를 위한 인터셉터. 페이징처리 후 자동정리 처리를 위해 생성
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-28
 * @see WebConfig
 */
@Component
public class PageClearInterceptor implements HandlerInterceptor {

    /**
     * 모든 요청 처리 완료 후 메모리 누수 방지를 위해 PageContext clear() 처리
     * @param request
     * @param response
     * @param handler
     * @param ex
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        PageContext.clear();  // ThreadLocal 정리
    }
}
