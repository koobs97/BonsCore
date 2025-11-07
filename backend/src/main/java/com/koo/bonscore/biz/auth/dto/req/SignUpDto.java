package com.koo.bonscore.biz.auth.dto.req;

import lombok.*;

import java.time.LocalDateTime;

/**
 * <pre>
 * SignUpDto.java
 * 설명 : 회원가입에 사용되는 dto 클래스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-07-22
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpDto {
    private String userId;
    private String password;
    private String userName;
    private String email;
    private String emailHash;
    private String phoneNumber;
    private String birthDate;
    private String genderCode;
    private String passwordUpdated;
    private String termsAgree1;
    private String termsAgree2;
    private String termsAgree3;
    private String termsAgree4;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 소셜로그인 변수
    private String oauthProvider;
    private String oauthProviderId;
}
