package com.koo.bonscore.common.api.google.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 정적 팩토리 메서드만 사용하도록 강제
public class StoreHoursResponseDto {
    private boolean open;                   // 현재 영업 여부
    private BusinessStatus businessStatus;  // "OPERATIONAL", "CLOSED_TEMPORARILY" 등
    private List<String> weekdayText;       // 주간 영업 시간 텍스트

    public static StoreHoursResponseDto success(boolean isOpen, String googleBusinessStatus, List<String> weekdayText) {
        return new StoreHoursResponseDto(isOpen, BusinessStatus.OPERATIONAL, weekdayText);
    }

    public static StoreHoursResponseDto noInfo() {
        return new StoreHoursResponseDto(false, BusinessStatus.NO_INFO, Collections.singletonList("i18n.openingInfo.noInfo"));
    }

    public static StoreHoursResponseDto notFound() {
        return new StoreHoursResponseDto(false, BusinessStatus.NOT_FOUND, Collections.singletonList("i18n.openingInfo.notFound"));
    }

    public static StoreHoursResponseDto closedPermanently() {
        return new StoreHoursResponseDto(false, BusinessStatus.CLOSED_PERMANENTLY, Collections.singletonList("i18n.openingInfo.closedPermanently"));
    }

    public static StoreHoursResponseDto apiError() {
        return new StoreHoursResponseDto(false, BusinessStatus.API_ERROR, Collections.singletonList("i18n.openingInfo.apiError"));
    }
}