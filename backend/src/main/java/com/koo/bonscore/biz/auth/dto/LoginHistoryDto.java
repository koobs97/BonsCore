package com.koo.bonscore.biz.auth.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * <pre>
 * LoginHistoryDto.java
 * 설명 : 로그인 기록 적재 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-10-27
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistoryDto {
    private String userId;
    private String ipAddress;
    private String userAgent;
    private String countryCode;
    private String cityName;
}
