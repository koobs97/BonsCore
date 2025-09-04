package com.koo.bonscore.common.api.kma.holiday.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayResponseDto {
    private String todayDayOfWeek; // 오늘 요일 (예: "MONDAY", "SUNDAY")
    private String holidayType;    // 휴일 타입 (예: "평일", "금요일", "휴일", "공휴일")
    private boolean isHolidayOrWeekend; // 오늘이 주말 또는 공휴일인지 여부 (편의상 추가)
}
