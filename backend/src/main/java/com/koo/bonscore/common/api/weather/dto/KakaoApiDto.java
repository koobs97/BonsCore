package com.koo.bonscore.common.api.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoApiDto {

    @JsonProperty("documents")
    private List<Document> documents;

    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        @JsonProperty("x")
        private String x; // 경도 (Longitude)

        @JsonProperty("y")
        private String y; // 위도 (Latitude)
    }
}
