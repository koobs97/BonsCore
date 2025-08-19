package com.koo.bonscore.common.api.naver;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "naver.client")
public class NaverProperties {

    private String id;
    private String secret;

}
