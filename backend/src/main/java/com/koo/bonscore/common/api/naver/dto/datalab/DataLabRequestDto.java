package com.koo.bonscore.common.api.naver.dto.datalab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DataLabRequestDto {
    private String startDate;
    private String endDate;
    private String timeUnit;
    private List<KeywordGroup> keywordGroups;
    // 필요하다면 device, gender, ages 등 추가 가능
}