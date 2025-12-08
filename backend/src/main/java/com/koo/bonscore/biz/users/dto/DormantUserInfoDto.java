package com.koo.bonscore.biz.users.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * <pre>
 * DormantUserInfoDto.java
 * 설명 : 휴먼유저정보 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-10-31
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DormantUserInfoDto {
    private String userId;                // 사용자 ID
    private String userName;              // 사용자 이름
    private String email;                 // 사용자 이메일
    private String emailHash;             // 사용자 이메일 해쉬
    private String phoneNumber;           // 사용자 전화번호
    private String birthDate;             // 사용자 생년월일
    private String genderCode;            // 성별 코드
    private String passwordHint;          // 비밀번호 힌트
    private String passwordHintAnswer;    // 비밀번호 힌트 정답
    private String passwordUpdated;       // 비밀번호 변경일자
    private String termsAgree1;           // 약관 동의 여부1
    private String termsAgree2;           // 약관 동의 여부2
    private String termsAgree3;           // 약관 동의 여부3
    private String termsAgree4;           // 약관 동의 여부4
    private LocalDateTime lastLoginAt;
}
