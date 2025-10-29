package com.koo.bonscore.biz.auth.dto.res;

import lombok.*;

/**
 * <pre>
 * LoginResponseDto.java
 * 설명 : 로그인 결과 반환 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-01
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private Boolean success;
    private String message;
    private String accessToken;
    private String refreshToken;
    private String reason;
}
