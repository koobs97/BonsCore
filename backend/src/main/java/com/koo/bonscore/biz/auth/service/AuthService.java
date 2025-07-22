package com.koo.bonscore.biz.auth.service;

import com.koo.bonscore.biz.auth.controller.RSAController;
import com.koo.bonscore.biz.auth.dto.UserDto;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.auth.dto.res.LoginResponseDto;
import com.koo.bonscore.biz.auth.mapper.AuthMapper;
import com.koo.bonscore.core.config.enc.EncryptionService;
import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <pre>
 * AuthService.java
 * 설명 : 권한(로그인, 로그아웃 등) 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-01-13
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final RSAController rsaController;
    private final BCryptPasswordEncoder passwordEncoder; // SecurityConfig에서 bean 생성
    private final AuthMapper authMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final EncryptionService encryptionService;

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
                      HttpStatusCode.INTERNAL_SERVER_ERROR
                    , ErrorCode.INVALID_CREDENTIALS
                    , ErrorCode.INVALID_CREDENTIALS.getMessage());
        }

        // 유저정보 세팅
        UserDto user = authMapper.findByUserId(request);
        UserDto decryptedUser = UserDto.builder()
                .userId(user.getUserId())
                .userName(encryptionService.decrypt(user.getUserName()))
                .email(encryptionService.decrypt(user.getEmail()))
                .phoneNumber(encryptionService.decrypt(user.getPhoneNumber()))
                .birthDate(encryptionService.decrypt(user.getBirthDate()))
                .genderCode(user.getGenderCode())
                .build();

        // Access Token: 15분
        String accessToken = jwtTokenProvider.createToken(decryptedUser.getUserId(), JwtTokenProvider.ACCESS_TOKEN_VALIDITY);
        // Refresh Token: 7일
        String refreshToken = jwtTokenProvider.createToken(decryptedUser.getUserId(), JwtTokenProvider.REFRESH_TOKEN_VALIDITY);

        response.setSuccess(true);
        response.setMessage("로그인 성공");
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken); // 임시 저장

        return response;
    }

    /**
     * 아이디 중복체크
     * @param request
     * @return
     */
    public boolean isDuplicateId(SignUpDto request) {
        return authMapper.existsById(request) > 0;
    }

    /**
     * 이메일 중복 체크
     * @param request
     * @return
     */
    public boolean isDuplicateEmail(SignUpDto request) {
        request.setEmail(encryptionService.encrypt(request.getEmail()));
        return authMapper.existsByEmail(request) > 0;
    }

    /**
     * 회원가입
     * @param request
     * @throws Exception
     */
    public void signup(SignUpDto request) throws Exception {

        SignUpDto item = SignUpDto.builder()
                .userId(request.getUserId())
                .userName(encryptionService.encrypt(request.getUserName()))
                .password(passwordEncoder.encode(rsaController.decrypt(request.getPassword())))
                .email(encryptionService.encrypt(request.getEmail()))
                .phoneNumber(encryptionService.encrypt(request.getPhoneNumber()))
                .birthDate(encryptionService.encrypt(request.getBirthDate()))
                .genderCode(request.getGenderCode())
                .passwordUpdated(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .termsAgree1(request.getTermsAgree1())
                .termsAgree2(request.getTermsAgree2())
                .termsAgree3(request.getTermsAgree3())
                .termsAgree4(request.getTermsAgree4())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        authMapper.signUpUser(item);
    }
}
