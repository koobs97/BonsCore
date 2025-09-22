package com.koo.bonscore.biz.store.dto.res;

import lombok.Getter;
import lombok.Setter;

/**
 * 맛집 목록 조회를 위한 간단한 정보 응답 DTO
 */
@Getter
@Setter
public class StoreSimpleResponseDto {
    private Long id;
    private String name;
    private String category;
    private String visitDate;
}
