package com.koo.bonscore.biz.users.dto.res;

import lombok.*;

/**
 * <pre>
 * UserInfoDto.java
 * 설명 : 유저정보 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-16
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {
    private String userId;
    private String userName;
    private String email;
    private String phoneNumber;
    private String birthDate;
    private String genderCode;
    private String loginTime;
    private String roleId;
}
