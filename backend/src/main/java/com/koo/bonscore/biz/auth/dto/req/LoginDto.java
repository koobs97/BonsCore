package com.koo.bonscore.biz.auth.dto.req;

import lombok.*;

/**
 * <pre>
 * LoginDto.java
 * 설명 : 로그인에 사용되는 request dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-03-25
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {
    private String userId;
    private String password;
    private boolean force = false; // 중복로그인 시 강제로그인 여부
}
