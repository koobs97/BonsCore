package com.koo.bonscore.common.api.naver.dto.datalab;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@NoArgsConstructor
public class Result {
    private String title;
    private List<String> keywords;
    private List<Data> data;
}