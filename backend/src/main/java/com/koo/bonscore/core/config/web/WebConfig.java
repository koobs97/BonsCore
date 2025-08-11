package com.koo.bonscore.core.config.web;

import com.koo.bonscore.common.paging.interceptor.PageClearInterceptor;
import com.koo.bonscore.log.interceptor.UserActivityLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * <pre>
 * WebConfig.java
 * 설명 : Spring Web MVC의 핵심 설정을 커스터마이징하는 클래스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-29
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PageClearInterceptor pageClearInterceptor;
    private final UserActivityLogInterceptor userActivityLogInterceptor;

    /**
     * 애플리케이션의 인터셉터 체인(Interceptor Chain)에 커스텀 인터셉터를 등록.
     * 등록된 순서대로 인터셉터가 실행된다.
     *
     * @param registry 인터셉터를 등록하고 URL 패턴을 매핑하는 데 사용되는 등록기(registry) 객체
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. PageClearInterceptor: 모든 경로(/**)에 적용
        registry.addInterceptor(pageClearInterceptor)
                .addPathPatterns("/**");

        // 2. UserActivityLogInterceptor: '/api/auth/**' 경로에만 적용
        registry.addInterceptor(userActivityLogInterceptor)
                .addPathPatterns("/api/auth/**");
    }
}
