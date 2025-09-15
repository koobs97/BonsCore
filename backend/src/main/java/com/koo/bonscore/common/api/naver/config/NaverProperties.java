package com.koo.bonscore.common.api.naver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "api.naver.client")
public class NaverProperties {

    private String id;
    private String secret;

}
