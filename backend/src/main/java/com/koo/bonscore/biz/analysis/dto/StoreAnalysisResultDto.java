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
    private int blogReviewCount; // 기존 블로그 리뷰 수
    private int timeScore;       // 시간/요일 기반 점수
    private int blogReviewScore; // 블로그 리뷰 수 기반 점수
}
