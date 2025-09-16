package com.koo.bonscore.common.api.kakao.surround.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SurroundingDataDto {
    private int hotPlaceCount;      // 핫플레이스 개수 (맛집, 카페, 영화관 등)
    private int competitorCount;    // 경쟁 가게 개수
    private int subwayStationCount; // 지하철역 개수
    private int universityCount;    // 대학교 개수
    private int officeBuildingCount; // 오피스 빌딩 개수
}
