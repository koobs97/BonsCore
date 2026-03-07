package com.koo.bonscore.biz.auth.dto.req;

import lombok.*;

/**
 * <pre>
 * ClientInfoDto.java
 * 설명 : 로그인 시 클라이언트 정보 get dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-12-10
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientInfoDto {
    private String userId;
    private String ipAddress;
    private String userAgent;
    private String countryCode;
    private String cityName;
}
