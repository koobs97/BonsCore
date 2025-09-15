package com.koo.bonscore.common.api.kma.subway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubwayCongestionDto {
    private boolean available;      // 데이터 조회 가능 여부
    private String stationName;     // 분석에 사용된 지하철역 이름
    private String congestionLevel; // 혼잡도 레벨 (예: "혼잡", "여유")
    private int score;              // 웨이팅 지수에 반영될 점수
}
