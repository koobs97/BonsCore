package com.koo.bonscore.biz.auth.dto;

import lombok.*;

import java.sql.Timestamp;

/**
 * UserDto
 * <p>
 * 사용자 정보를 담는 데이터 전송 객체
 * 로그인 성공 후 사용자 기본 정보를 응답하거나,
 * 회원정보 조회/수정 시 사용
 * <p>
 * Fields:
 * - userId       : 사용자 ID
 * - userName     : 사용자 이름
 * - email        : 이메일 주소
 * - phoneNumber  : 전화번호
 * - birthDate    : 생년월일 (yyyyMMdd 형식)
 * - genderCode   : 성별 코드 (M, F)
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String userId;
    private String userName;
    private String email;
    private String phoneNumber;
    private String birthDate;
    private String genderCode;
}
