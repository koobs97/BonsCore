package com.koo.bonscore.biz.users.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserReqIdDto {
    @NotBlank(message = "아이디를 입력해주세요.")
    private String userId;
}
