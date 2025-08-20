package com.koo.bonscore.common.api.naver.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NaverApiResponseDto {
    private int total;
    private List<NaverItemDto> items;
}
