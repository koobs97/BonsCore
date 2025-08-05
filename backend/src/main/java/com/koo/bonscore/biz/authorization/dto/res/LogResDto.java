package com.koo.bonscore.biz.authorization.dto.res;

import lombok.*;

/**
 * <pre>
 * LogResDto.java
 * 설명 : 관리자의 로그 조회 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-05
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogResDto {
    private String logId;
    private String userId;
    private String activityType;
    private String activityResult;
    private String requestIp;
    private String requestUri;
    private String requestMethod;
    private String errorMessage;
    private String userAgent;
    private String createdAt;
}
