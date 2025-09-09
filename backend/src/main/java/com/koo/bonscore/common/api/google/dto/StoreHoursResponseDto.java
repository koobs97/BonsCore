package com.koo.bonscore.common.api.google.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StoreHoursResponseDto {
    private boolean open;           // 현재 영업 여부
    private String businessStatus;  // "OPERATIONAL", "CLOSED_TEMPORARILY" 등
    private List<String> weekdayText; // 주간 영업 시간 텍스트
}