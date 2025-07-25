package com.koo.bonscore.biz.auth.service;

import com.koo.bonscore.biz.auth.controller.RSAController;
import com.koo.bonscore.biz.auth.dto.UserDto;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.auth.dto.req.UserInfoSearchDto;
import com.koo.bonscore.biz.auth.dto.res.LoginResponseDto;
import com.koo.bonscore.biz.auth.mapper.AuthMapper;
import com.koo.bonscore.common.mail.service.MailService;
import com.koo.bonscore.core.config.enc.EncryptionService;
import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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

    // 인증 mapper
    private final AuthMapper authMapper;

    // 암호화 관련
    private final RSAController rsaController;
    private final BCryptPasswordEncoder passwordEncoder; // SecurityConfig에서 bean 생성
    private final EncryptionService encryptionService;

    // 인증 컴포넌트
    private final JwtTokenProvider jwtTokenProvider;

    /* 메일 인증 관련 서비스 */
    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;
    private static final String VERIFICATION_PREFIX = "verification:";
    private static final long EXPIRATION_MINUTES = 3;

    /**
     * 로그인 서비스
     * @param request
     * @return LoginResponseDto
     * @throws Exception
     */
    @Transactional
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
        request.setEmailHash(encryptionService.hashWithSalt(request.getEmail()));
        return authMapper.existsByEmail(request) > 0;
    }

    /**
     * 회원가입
     * @param request
     * @throws Exception
     */
    @Transactional
    public void signup(SignUpDto request) throws Exception {
        // 회원가입 컬럼 조립
        // 암호화 대상 : 유저명, 패스워드, 이메일, 전화번호, 생년월일
        SignUpDto item = SignUpDto.builder()
                .userId(request.getUserId())
                .userName(encryptionService.encrypt(request.getUserName()))
                .password(passwordEncoder.encode(rsaController.decrypt(request.getPassword())))
                .email(encryptionService.encrypt(request.getEmail()))
                .emailHash(encryptionService.hashWithSalt(request.getEmail()))
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

        // 로그저장
        authMapper.signUpUser(item);
    }

    /**
     * 아이디/비밀번호 찾기 서비스
     * @param request
     * @throws Exception
     */
    public void searchIdBySendMail(UserInfoSearchDto request) throws Exception {

        String type = request.getType();

        // 형식(Format)검증 - 아이디/비밀번호 찾기 공통 입력값
        if(StringUtils.isEmpty(request.getUserName()))
            throw new BsCoreException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR
                    , ErrorCode.INVALID_INPUT
                    , "유저명을 입력해주세요.");
        if(StringUtils.isEmpty(request.getEmail()))
            throw new BsCoreException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR
                    , ErrorCode.INVALID_INPUT
                    , "이메일을 입력해주세요.");

        /* 아이디 찾기 일 때 */
        if("ID".equals(type)) {

            // 존재(Existence) 검증
            UserInfoSearchDto input = UserInfoSearchDto.builder()
                    .email(encryptionService.hashWithSalt(request.getEmail()))
                    .build();

            // 이메일과 일치하는 정보 조회 후 복호화하여 유저명도 비교
            String userName = authMapper.findByUserNameAndEmail(input);
            if(userName == null || !encryptionService.decrypt(userName).equals(request.getUserName()))
                throw new BsCoreException(
                        HttpStatusCode.INTERNAL_SERVER_ERROR
                        , ErrorCode.INVALID_INPUT
                        , "입력하신 정보와 일치하는 사용자가 없습니다.");

        } // ID
        
        /* 비밀번호 찾기 일 때 */
        if("PW".equals(type)) {

            // 형식(Format)검증
            if(StringUtils.isEmpty(request.getNonMaskedId()))
                throw new BsCoreException(
                        HttpStatusCode.INTERNAL_SERVER_ERROR
                        , ErrorCode.INVALID_INPUT
                        , "아이디를 입력해주세요.");

            UserInfoSearchDto result = authMapper.findUserById(request);
            if(result != null) {
                result.setUserName(encryptionService.decrypt(result.getUserName()));
                result.setEmail(encryptionService.decrypt(result.getEmail()));
                if(!result.getUserName().equals(request.getUserName()) || !result.getEmail().equals(request.getEmail())) {
                    throw new BsCoreException(
                            HttpStatusCode.INTERNAL_SERVER_ERROR
                            , ErrorCode.INVALID_INPUT
                            , "입력하신 정보와 일치하는 사용자가 없습니다.");
                }
            }

        } // PW

        // 메일 전송
        String authCode = mailService.sendVerificationEmail(request.getEmail(), request.getUserName());

        // redis에 저장
        String key = VERIFICATION_PREFIX + request.getEmail();
        redisTemplate.opsForValue().set(key, authCode, Duration.ofMinutes(EXPIRATION_MINUTES));

    }

    /**
     * 인증코드 인증
     * @param email
     * @param code
     * @return
     */
    public UserInfoSearchDto verifyCode(String email, String code) throws Exception {

        String key = VERIFICATION_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode != null && storedCode.equals(code)) {
            // 인증 성공 시, 즉시 코드를 삭제하여 재사용을 방지.
            redisTemplate.delete(key);

            // 2. 유저 조회
            UserInfoSearchDto input = UserInfoSearchDto.builder()
                    .email(encryptionService.hashWithSalt(email))
                    .build();

            // 이메일과 일치하는 정보 조회 후 복호화하여 유저명도 비교
            String userId = authMapper.findByUserIdByMail(input);

            return UserInfoSearchDto.builder()
                    .userId(userId)
                    // 만료시간 15분 -> 비밀번호 찾기 -> 비밀번호 변경에서 사용할 토큰
                    .token(jwtTokenProvider.createToken(userId, JwtTokenProvider.ACCESS_TOKEN_VALIDITY))
                    .build();
        } else {
            throw new BsCoreException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR
                    , ErrorCode.INTERNAL_SERVER_ERROR
                    , "인증 코드가 유효하지 않거나 만료되었습니다.");
        }
    }

    /**
     * 유저 ID 복사 시 호출
     * @param request
     * @return
     * @throws Exception
     */
    public String searchIdByMail(UserInfoSearchDto request) throws Exception {
        UserInfoSearchDto input = UserInfoSearchDto.builder()
                .email(encryptionService.hashWithSalt(request.getEmail()))
                .build();
        return authMapper.findByUserIdByMail(input);
    }

    /**
     * 비밀번호 업데이트
     * @param token
     * @param newPassword
     * @throws Exception
     */
    @Transactional
    public void resetPasswordWithToken(String token, String newPassword) throws Exception {

        // 1. 토큰 유효성 검증 (만료 여부, 위변조 여부 등)
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BsCoreException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR
                    , ErrorCode.INTERNAL_SERVER_ERROR
                    , "토큰이 유효하지 않거나 만료되었습니다.");
        }

        // 2. 토큰에서 사용자 ID 추출
        String userId = jwtTokenProvider.getUserId(token);
        UserInfoSearchDto user = UserInfoSearchDto.builder()
                .nonMaskedId(userId)
                .build();

        // 3. 사용자 유효성 검증
        UserInfoSearchDto result = authMapper.findUserById(user);
        if(result == null) {
            throw new BsCoreException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR
                    , ErrorCode.INVALID_INPUT
                    , "입력하신 정보와 일치하는 사용자가 없습니다.");
        }

        // 4. 입력값 세팅
        UserInfoSearchDto updateInput = UserInfoSearchDto.builder()
                .userId(userId)
                .password(passwordEncoder.encode(rsaController.decrypt(newPassword)))
                .updatedAt(LocalDateTime.now())
                .build();

        // 5. 비밀번호 업데이트
        authMapper.updatePassword(updateInput);

    }

}
