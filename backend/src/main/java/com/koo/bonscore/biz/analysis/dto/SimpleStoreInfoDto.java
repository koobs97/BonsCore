package com.koo.bonscore.biz.analysis.dto;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SimpleStoreInfoDto {
    private long id;
    private String name;
    private String simpleAddress;
    private String detailAddress;
}
