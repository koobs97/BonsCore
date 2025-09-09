package com.koo.bonscore.common.api.google.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class GooglePlaceDetailsResponseDto {
    private PlaceDetailsResult result;
    private String status;

    @Data
    @NoArgsConstructor
    public static class PlaceDetailsResult {
        @JsonProperty("business_status")
        private String businessStatus; // e.g., "OPERATIONAL"

        @JsonProperty("opening_hours")
        private OpeningHours openingHours;
    }

    @Data
    @NoArgsConstructor
    public static class OpeningHours {
        @JsonProperty("open_now")
        private boolean openNow;

        @JsonProperty("weekday_text")
        private List<String> weekdayText; // ["월요일: 오전 10:00 ~ 오후 9:00", ...]
    }
}