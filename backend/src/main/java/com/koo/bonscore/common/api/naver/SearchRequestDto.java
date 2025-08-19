package com.koo.bonscore.common.api.naver;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequestDto {
    private String query;
    private int start = 1;
}