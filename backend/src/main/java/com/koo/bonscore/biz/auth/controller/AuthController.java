package com.koo.bonscore.biz.auth.controller;

import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.res.LoginResponseDto;
import com.koo.bonscore.biz.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginDto request) throws Exception {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
