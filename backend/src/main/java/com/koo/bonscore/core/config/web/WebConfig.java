package com.koo.bonscore.core.config.web;

import com.koo.bonscore.common.paging.interceptor.PageClearInterceptor;
import com.koo.bonscore.log.interceptor.UserActivityLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PageClearInterceptor pageClearInterceptor;
    private final UserActivityLogInterceptor userActivityLogInterceptor;

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
