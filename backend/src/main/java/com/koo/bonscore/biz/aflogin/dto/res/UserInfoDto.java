package com.koo.bonscore.biz.aflogin.dto.res;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class UserInfoDto {
    private String userId;
    private String userName;
    private String email;
    private String phoneNumber;
    private String birthDate;
    private String genderCode;
    private String loginTime;
}
