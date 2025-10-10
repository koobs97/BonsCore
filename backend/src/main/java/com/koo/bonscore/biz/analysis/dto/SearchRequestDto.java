package com.koo.bonscore.biz.analysis.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 * SearchRequestDto.java
 * 설명 : 웨이팅 예측 분석 - 초기 가게 목록 검색 API에 사용되는 request dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-19
 */
@Getter
@Setter
public class SearchRequestDto {
    private String query;
    private int start = 1;
}