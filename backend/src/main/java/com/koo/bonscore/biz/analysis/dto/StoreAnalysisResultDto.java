package com.koo.bonscore.biz.analysis.dto;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class StoreAnalysisResultDto {
    // 요청받은 가게 정보
    private String name;
    private String simpleAddress;
    private String detailAddress;

    // 분석 결과
    private int blogReviewCount;
    // private double weatherScore; // 예: 추후 날씨 점수 등 확장 가능
}
