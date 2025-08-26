package com.koo.bonscore.common.api.weather.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherResponseDto {
    private String temperature; // 현재 기온 (TMP)
    private String sky;         // 하늘 상태 (SKY)
    private String precipitation; // 강수 형태 (PTY)
}
