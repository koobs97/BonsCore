package com.koo.bonscore.common.api.naver.dto.datalab;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@NoArgsConstructor
public class DataLabResponseDto {
    private String startDate;
    private String endDate;
    private String timeUnit;
    private List<Result> results;
}
