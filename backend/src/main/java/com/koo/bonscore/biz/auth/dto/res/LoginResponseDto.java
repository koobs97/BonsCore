package com.koo.bonscore.biz.auth.dto.res;

import lombok.Data;

@Data
public class LoginResponseDto {
    private Boolean success;
    private String message;
    private String accessToken;
    private String refreshToken;
}
