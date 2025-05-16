package com.koo.bonscore.biz.aflogin.controller;

import com.koo.bonscore.biz.aflogin.dto.req.UserReqIdDto;
import com.koo.bonscore.biz.aflogin.dto.res.UserInfoDto;
import com.koo.bonscore.biz.aflogin.service.UserInfo;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("api/aflogin")
@RequiredArgsConstructor
public class LoginSuccessController {

    private final UserInfo userInfo;

    @PostMapping("/afterLogin")
    public void afterLogin(@RequestBody LoginDto request) throws Exception {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        System.err.println("╔══════════════════════════════════════════════════╗");
        System.err.printf ("║ ✅ LOGIN SUCCESS                                ║%n");
        System.err.println("╟──────────────────────────────────────────────────╢");
        System.err.printf ("║ Time   : %s%n", time);
        System.err.println("╚══════════════════════════════════════════════════╝");
    }

    @PostMapping("/me")
    public UserInfoDto loginSuccess(@RequestBody UserReqIdDto request) {
        return userInfo.getUserInfo(request);
    }

}

