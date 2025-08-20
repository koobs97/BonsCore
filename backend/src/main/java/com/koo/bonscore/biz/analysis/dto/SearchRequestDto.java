package com.koo.bonscore.biz.analysis.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequestDto {
    private String query;
    private int start = 1;
}