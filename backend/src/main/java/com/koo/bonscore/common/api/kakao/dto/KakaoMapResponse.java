package com.koo.bonscore.common.api.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoMapResponse {
    private Meta meta;
    private List<Document> documents;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("total_count")
        private int totalCount;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        @JsonProperty("place_name")
        private String placeName;

        @JsonProperty("category_group_code")
        private String categoryGroupCode;

        @JsonProperty("category_name")
        private String categoryName;

        @JsonProperty("x") // longitude
        private String x;

        @JsonProperty("y") // latitude
        private String y;
    }
}
