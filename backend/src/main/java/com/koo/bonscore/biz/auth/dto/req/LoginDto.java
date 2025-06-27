package com.koo.bonscore.biz.auth.dto.req;

import lombok.Data;

@Data
public class LoginDto {
    private String userId;
    private String password;
    private boolean force = false;
}
