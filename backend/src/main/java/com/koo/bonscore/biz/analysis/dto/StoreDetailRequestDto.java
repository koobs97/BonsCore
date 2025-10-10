package com.koo.bonscore.biz.analysis.dto;

import lombok.*;

/**
 * <pre>
 * StoreAnalysisResultDto.java
 * 설명 : 웨이팅 예측 분석 - 네이버 가게 상세분석 request dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-19
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class StoreDetailRequestDto {
    private String name;
    private String simpleAddress;
    private String detailAddress;

    private String selectedTime;
}
