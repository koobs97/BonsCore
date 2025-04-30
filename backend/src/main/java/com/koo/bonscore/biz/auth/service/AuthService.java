package com.koo.bonscore.biz.auth.service;

import com.koo.bonscore.biz.auth.controller.RSAController;
import com.koo.bonscore.biz.auth.dto.UserDto;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.res.LoginResponseDto;
import com.koo.bonscore.biz.auth.mapper.AuthMapper;
import com.koo.bonscore.core.config.web.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final RSAController rsaController;
    private final BCryptPasswordEncoder passwordEncoder; // SecurityConfig에서 bean 생성
    private final AuthMapper authMapper;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 로그인 서비스
     * @param request
     * @return LoginResponseDto
     * @throws Exception
     */
    public LoginResponseDto login(LoginDto request) throws Exception {

        LoginResponseDto response = new LoginResponseDto();

        // RSA 복호화
        String decryptedPassword = rsaController.decrypt(request.getPassword());

        // bcrypt 해싱 후 검증
        // String hashedPassword = passwordEncoder.encode(decryptedPassword);

        // userId의 해싱된 passwd get
        String getHasedPassword = authMapper.login(request);

        // 비밀번호 비교는 matches 함수 사용
        boolean isMatch = passwordEncoder.matches(decryptedPassword, getHasedPassword);

        if (!isMatch) {
            response.setSuccess(Boolean.FALSE);
            response.setMessage("비밀번호가 일치하지 않습니다.");
            return response;
        }

        // 유저정보 세팅
        UserDto user = authMapper.findByUserId(request);

        String token = jwtTokenProvider.createToken(user.getUserId());
        response.setSuccess(true);
        response.setMessage("로그인 성공");
        response.setToken(token);

        return response;

    }
}
