package com.koo.bonscore.biz.auth.dto.res;

import lombok.Data;

@Data
public class LoginResponseDto {
    private boolean success;
    private String message;
    private String token;
}
