package com.koo.bonscore.biz.auth.dto.req;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
public class UserInfoSearchDto {
    private String userName;
    private String userId;
    private String email;
}
