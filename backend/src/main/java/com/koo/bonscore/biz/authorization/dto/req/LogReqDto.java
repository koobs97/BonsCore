package com.koo.bonscore.biz.authorization.dto.req;

import lombok.*;

/**
 * <pre>
 * LogReqDto.java
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
public class LogReqDto {
    private String startDt;
    private String endDt;
    private String userId;
    private String activityType;
    private String activityResult;
}
