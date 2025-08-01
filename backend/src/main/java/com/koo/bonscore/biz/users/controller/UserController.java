package com.koo.bonscore.biz.users.controller;

import com.koo.bonscore.biz.users.dto.req.UserReqIdDto;
import com.koo.bonscore.biz.users.dto.res.UserInfoDto;
import com.koo.bonscore.biz.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <pre>
 * UserController.java
 * 설명 : 사용자 정보 관리 - "인증된 사용자의 정보 조회 및 수정"
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-01
 */
@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userInfo;

    @PostMapping("/me")
    public UserInfoDto loginSuccess(@RequestBody UserReqIdDto request) {
        return userInfo.getUserInfo(request);
    }

}
