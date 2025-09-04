package com.koo.bonscore.common.api.kma.weather.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "api.kma")
@Getter
@Setter
public class KmaApiProperties {
    private String url;
    private String key;
}
