package com.koo.bonscore.biz.users.controller;

import com.koo.bonscore.biz.users.dto.DormantUserInfoDto;
import com.koo.bonscore.biz.users.dto.res.UserInfoDto;
import com.koo.bonscore.biz.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * <pre>
 * UserController.java
 * 설명 : 사용자 정보 관리
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
    public UserInfoDto loginSuccess(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다.");
        }
        String userId = userDetails.getUsername();
        return userInfo.getUserInfo(userId);
    }

    /**
     * 휴면 계정을 활성화한다
     * 본인인증 완료 후 프론트엔드에서 호출
     * @param request 활성화할 사용자 ID를 담은 DTO
     */
    @PostMapping("/activate-dormant")
    public void activateDormantUser(@RequestBody DormantUserInfoDto request) {
        userInfo.activateDormantUser(request);
    }

}
