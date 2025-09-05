package com.koo.bonscore.common.api.naver.dto.datalab;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class Data {
    private String period;
    private Double ratio; // 검색량 비율
}
