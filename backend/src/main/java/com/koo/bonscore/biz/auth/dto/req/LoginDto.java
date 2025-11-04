package com.koo.bonscore.biz.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "아이디를 입력해주세요.")
    private String userId;
    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    private boolean force = false; // 중복로그인 시 강제로그인 여부
    private String recaptchaToken; // reCAPTCHA 응답 토큰을 받기 위한 필드
}
