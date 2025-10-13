package com.koo.bonscore.biz.authorization.dto.req;

import lombok.*;

import java.time.LocalDateTime;

/**
 * <pre>
 * UpdateUserDto.java
 * 설명 : 회원정보 수정 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-09-01
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDto {
    private String userId;
    private String password;
    private String passwordUpdated;
    private String passwordHint;
    private String passwordHintAnswer;
    private String userName;
    private String email;
    private String emailHash;
    private String phoneNumber;
    private String birthDate;
    private String genderCode;
    private LocalDateTime updatedAt;
}
