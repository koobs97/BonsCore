package com.koo.bonscore.biz.auth.service;

import com.koo.bonscore.biz.auth.controller.RSAController;
import com.koo.bonscore.biz.auth.dto.UserDto;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.res.LoginResponseDto;
import com.koo.bonscore.biz.auth.mapper.AuthMapper;
import com.koo.bonscore.core.config.web.JwtTokenProvider;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.core.lmx.CoreException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDto login(LoginDto request) throws Exception {

        LoginResponseDto response = new LoginResponseDto();

        // RSA 복호화
        String decryptedPassword = rsaController.decrypt(request.getPassword());

        // bcrypt 해싱 후 검증
        String hashedPassword = passwordEncoder.encode(decryptedPassword);

        // userId의 해싱된 passwd get
        String getHasedPassword = authMapper.login(request);

        // 비밀번호 비교는 matches 함수 사용
        boolean isMatch = passwordEncoder.matches(decryptedPassword, getHasedPassword);

        if (!isMatch) {
            response.setSuccess(Boolean.FALSE);
            response.setMessage(ErrorCode.INVALID_CREDENTIALS.getMessage());

            throw new BsCoreException(
                      HttpStatusCode.UNAUTHORIZED
                    , ErrorCode.INVALID_CREDENTIALS
                    , ErrorCode.INVALID_CREDENTIALS.getMessage());
        }

        // 유저정보 세팅
        UserDto user = authMapper.findByUserId(request);

        // Access Token: 15분
        String accessToken = jwtTokenProvider.createToken(user.getUserId(), JwtTokenProvider.ACCESS_TOKEN_VALIDITY);
        // Refresh Token: 7일
        String refreshToken = jwtTokenProvider.createToken(user.getUserId(), JwtTokenProvider.REFRESH_TOKEN_VALIDITY);

        response.setSuccess(true);
        response.setMessage("로그인 성공");
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken); // 임시 저장

        return response;
    }
}
