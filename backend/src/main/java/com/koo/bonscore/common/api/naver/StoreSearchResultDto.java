package com.koo.bonscore.common.api.naver;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class StoreSearchResultDto {
    private long id;
    private String name;
    private String simpleAddress;
    private String detailAddress;
}
