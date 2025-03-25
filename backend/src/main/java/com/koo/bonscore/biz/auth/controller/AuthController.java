package com.koo.bonscore.biz.auth.controller;

import com.koo.bonscore.biz.auth.dto.LoginDto;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
public class AuthController {

    private final RSAController rsaController;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(RSAController rsaController) {
        this.rsaController = rsaController;
    }

    @PostMapping("/login")
    public void login(@RequestBody LoginDto request) throws Exception {

        System.out.println("password: " + request.getPassword());

        // RSA 복호화
        String decryptedPassword = rsaController.decrypt(request.getPassword());

        System.out.println("복호화된 비밀번호: " + decryptedPassword);

        // bcrypt 해싱 후 검증
        String hashedPassword = passwordEncoder.encode(decryptedPassword);
        System.out.println("해싱된 비밀번호: " + hashedPassword);

        String gethasedPassword = "$2a$10$K/0XlETHiAfciJ1t6X81JucW1hd5THnTIjR/NmAoaI56yhY/xFbSu";

        // 비밀번호 비교는 matches 함수 사용
        boolean isMatch = passwordEncoder.matches(decryptedPassword, gethasedPassword);
        System.out.println("비밀번호 일치 여부: " + isMatch);

        System.out.println("testtesttesttesttesttesttesttest");
        System.out.println("testtesttesttesttesttesttesttest");
        System.out.println("testtesttesttesttesttesttesttest");
        System.out.println("testtesttesttesttesttesttesttest");
    }
}
