package com.koo.bonscore.biz.analysis.dto;

import lombok.*;

/**
 * <pre>
 * StoreAnalysisResultDto.java
 * 설명 : 웨이팅 예측 분석 - 네이버 가게 상세분석 결과 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-20
 */
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
