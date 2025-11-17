package com.koo.bonscore.biz.analysis.dto;

import lombok.*;

/**
 * <pre>
 * SimpleStoreInfoDto.java
 * 설명 : 웨이팅 예측 분석 - 네이버블로그검색 결과 dto
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
public class SimpleStoreInfoDto {
    private long id;
    private String name;
    private String nameKo;
    private String simpleAddress;
    private String detailAddress;
    private String simpleAddressKo;
    private String detailAddressKo;
}
