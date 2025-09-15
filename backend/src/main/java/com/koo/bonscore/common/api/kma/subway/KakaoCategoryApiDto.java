package com.koo.bonscore.common.api.kma.subway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoCategoryApiDto {
    private List<Document> documents;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        @JsonProperty("place_name")
        private String placeName; // 장소명 (예: 강남역)

        @JsonProperty("category_name")
        private String categoryName; // 카테고리 (예: 교통,수송 > 지하철,전철 > 수도권2호선)

        private String x; // 경도
        private String y; // 위도
    }
}
