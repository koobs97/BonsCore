package com.koo.bonscore.common.api.naver.dto;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NaverItemDto {
    private String title;
    private String roadAddress;
    private String address;
}
