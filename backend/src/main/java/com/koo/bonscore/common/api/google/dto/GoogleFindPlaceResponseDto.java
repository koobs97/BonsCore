package com.koo.bonscore.common.api.google.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class GoogleFindPlaceResponseDto {
    private List<Candidate> candidates;
    private String status;

    @Data
    @NoArgsConstructor
    public static class Candidate {
        @JsonProperty("place_id")
        private String placeId;
    }
}