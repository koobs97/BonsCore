package com.koo.bonscore.core.config.web.security.filter;

import com.koo.bonscore.core.config.web.security.filter.XssEscapeFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    // resourceUrlPrefix 값을 application.yml에서 주입받기 위해 추가
    @org.springframework.beans.factory.annotation.Value("${file.resource-url-prefix}")
    private String resourceUrlPrefix; // "/images/"

    /**
     * XssEscapeFilter를 특정 URL에만 적용하도록 등록합니다.
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<XssEscapeFilter> xssEscapeFilterRegistrationBean() {
        FilterRegistrationBean<XssEscapeFilter> registrationBean = new FilterRegistrationBean<>();

        // 1. 등록할 필터를 설정합니다.
        registrationBean.setFilter(new XssEscapeFilter());

        // 2. 필터가 적용될 URL 패턴을 지정합니다.
        //    보통 API 요청에만 적용하면 되므로 "/api/*" 등으로 한정합니다.
        registrationBean.addUrlPatterns("/api/*");

        // 3. 필터의 순서를 지정합니다. (필요한 경우, 낮을수록 우선순위 높음)
        registrationBean.setOrder(1);

        return registrationBean;
    }

    // 참고: 만약 다른 필터(MaskingClearFilter 등)도 문제가 된다면
    //       비슷한 방식으로 FilterRegistrationBean을 만들어 등록하고
    //       URL 패턴을 조절할 수 있습니다.
}
