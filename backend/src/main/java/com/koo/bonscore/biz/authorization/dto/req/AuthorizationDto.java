package com.koo.bonscore.biz.authorization.dto.req;

import lombok.*;

/**
 * <pre>
 * AuthorizationService.java
 * 설명 : 인가 (Authorization) 관련 서비스 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-01
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationDto {
    private String userId;
}
