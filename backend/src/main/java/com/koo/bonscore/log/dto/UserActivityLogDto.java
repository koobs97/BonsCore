package com.koo.bonscore.log.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * <pre>
 * UserActivityLogDto.java
 * 설명 : 로그 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-07-23
 */
@Getter
@Builder
@ToString
public class UserActivityLogDto {
    private String userId;
    private String activityType;
    private String activityResult;
    private String requestIp;
    private String requestUri;
    private String requestMethod;
    private String errorMessage;
    private String userAgent;
}
