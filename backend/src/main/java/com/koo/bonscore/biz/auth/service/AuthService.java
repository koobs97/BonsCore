package com.koo.bonscore.biz.auth.service;

import com.koo.bonscore.biz.auth.controller.RSAController;
import com.koo.bonscore.biz.auth.dto.LoginCheckDto;
import com.koo.bonscore.biz.auth.dto.LoginHistoryDto;
import com.koo.bonscore.biz.auth.dto.UserDto;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.auth.dto.req.UserInfoSearchDto;
import com.koo.bonscore.biz.auth.dto.res.LoginResponseDto;
import com.koo.bonscore.biz.auth.mapper.AuthMapper;
import com.koo.bonscore.common.api.mail.service.MailService;
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
import java.util.List;

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
    private final LoginAttemptService loginAttemptService;
    private final PwnedPasswordService pwnedPasswordService;

    // 메일 인증 관련 서비스
    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;
    private static final String VERIFICATION_PREFIX = "verification:";
    private static final long EXPIRATION_MINUTES = 3;

    /**
     * 로그인 서비스
     * @param request 로그인에 필요한 사용자 ID와 비밀번호를 담은 DTO
     * @return LoginResponseDto 로그인 성공 여부, Access Token, 메시지 등을 포함하는 응답 DTO
     * @throws Exception 로그인 과정에서 발생하는 모든 예외
     */
    @Transactional
    public LoginResponseDto login(LoginDto request, LoginHistoryDto clientInfo) throws Exception {

        // 1. 사용자 정보 및 비밀번호 유효성 검증
        String decryptedPassword = rsaController.decrypt(request.getPassword());
        LoginCheckDto loginCheckDto = validateCredentials(request, decryptedPassword);

        // 2. 휴면 계정 여부 확인
        if (isDormantAccount(loginCheckDto)) {
            return createDormantAccountResponse();
        }

        // 3. 비정상 로그인 탐지
        boolean isAbnormal = checkAbnormalLogin(request, clientInfo, loginCheckDto);
        if (isAbnormal) {
            // 비정상 로그인 시, 추가 인증 요구 응답 반환
            return createAbnormalLoginResponse(request.getUserId());
        }

        // 4. 로그인 성공 처리
        processLoginSuccess(request, clientInfo);

        // 5. 토큰 발급 및 최종 응답 생성
        return createSuccessResponse(request);
    }

    /**
     * 1. 사용자 자격 증명을 확인
     */
    private LoginCheckDto validateCredentials(LoginDto request, String decryptedPassword) {
        LoginCheckDto loginCheckDto = authMapper.login(request);
        if(loginCheckDto == null) {

            throw new BsCoreException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR
                    , ErrorCode.INVALID_CREDENTIALS
                    , ErrorCode.INVALID_CREDENTIALS.getMessage());
        }

        String getHasedPassword = loginCheckDto.getPasswordHash();

        // 비밀번호 비교는 matches 함수 사용
        boolean isMatch = passwordEncoder.matches(decryptedPassword, getHasedPassword);

        if (!isMatch) {
            throw new BsCoreException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR
                    , ErrorCode.INVALID_CREDENTIALS
                    , ErrorCode.INVALID_CREDENTIALS.getMessage());
        }

        return loginCheckDto;
    }

    /**
     * 2. 휴먼계정인지 확인
     */
    private boolean isDormantAccount(LoginCheckDto loginCheckDto) {
        return loginCheckDto.getAccountLocked().equals("Y");
    }

    /**
     * 2-1. 휴먼계정 응답객체 생성
     */
    private LoginResponseDto createDormantAccountResponse() {
        return LoginResponseDto.builder()
                .success(false)
                .reason("DORMANT_ACCOUNT")
                .message("장기간 미접속으로 계정이 휴면 상태로 전환되었습니다.<br>본인인증을 통해 계정을 활성화할 수 있습니다.")
                .build();
    }

    /**
     * 3. 비정상 로그인 탐지
     */
    private boolean checkAbnormalLogin(LoginDto loginDto, LoginHistoryDto clientInfo, LoginCheckDto loginCheckDto) {
        try {
            // 비정상 로그인으로 계정이 잠겨있는지 확인
            if("Y".equals(loginCheckDto.getRequiresVerificationYn())) {
                return true;
            }

            // Redis에서 최근 접속 국가 정보 조회
            String redisKey = "last_login_country:" + loginDto.getUserId();
            String lastCountry = redisTemplate.opsForValue().get(redisKey);
            log.info("redisKey: {}, lastCountry: {}", redisKey, lastCountry);

            if (lastCountry != null && lastCountry.equals(clientInfo.getCountryCode())) {
                return false; // 최근 접속 국가와 동일하면 정상으로 간주
            }

            // Redis에 정보가 없거나 국가가 다르면 DB에서 전체 기록 조회
            List<String> recentCountries = authMapper.findRecentLoginCountries(loginDto.getUserId()); // Mapper에 추가 필요
            log.info("recentCountries: {}", recentCountries);
            log.info("clientInfo.getCountryCode(): {}", clientInfo.getCountryCode());

            if (recentCountries.isEmpty() || !recentCountries.contains(clientInfo.getCountryCode())) {
                // 계정 잠금 처리
                authMapper.updateRequiresYn(loginDto);
                log.warn("비정상 로그인 시도 감지: User ID = {}, IP = {}, Country = {}", loginDto.getUserId(), clientInfo.getIpAddress(), clientInfo.getCountryCode());

                return true;
            }

            return false;
        } catch (Exception e) {
            // IP 조회 실패 등 예외 발생 시 일단 정상으로 처리하여 로그인 흐름을 막지 않음
            log.error("비정상 로그인 탐지 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 3-1. 비정상 로그인 시 응답 객체 생성
     */
    private LoginResponseDto createAbnormalLoginResponse(String userId) {
        return LoginResponseDto.builder()
                .success(false)
                .reason("ACCOUNT_VERIFICATION_REQUIRED")
                .message("다른 환경에서의 로그인이 감지되었습니다.<br>계정 보호를 위해 본인인증을 진행해주세요.")
                .build();
    }

    /**
     * 4. 로그인 성공처리 수행
     */
    private void processLoginSuccess(LoginDto request, LoginHistoryDto clientInfo) {
        authMapper.updateLoginAt(request);
        loginAttemptService.loginSucceeded(request.getUserId());

        saveLoginHistory(request.getUserId(), clientInfo);
    }

    /**
     * 4-1. 로그인 로그 저장
     */
    private void saveLoginHistory(String userId, LoginHistoryDto history) {
        try {
            // Mapper에 전달할 DTO나 Map 생성
            authMapper.insertLoginHistory(history); // Mapper에 추가 필요

            // Redis에 최근 접속 국가 정보 업데이트 (TTL 설정 가능)
            String redisKey = "last_login_country:" + userId;
            redisTemplate.opsForValue().set(redisKey, history.getCountryCode(), Duration.ofDays(30)); // 예: 30일간 유지

        } catch (Exception e) {
            log.error("로그인 기록 저장 중 오류 발생", e);
        }
    }

    /**
     * 6. 최종 성공 응답과 토큰을 생성
     */
    private LoginResponseDto createSuccessResponse(LoginDto request) {

        // 사용자 ID로 전체 정보 조회
        UserDto user = authMapper.findByUserId(request);

        // 추가 정보 필요 여부 확인
        boolean additionalInfoRequired = StringUtils.isBlank(user.getBirthDate())
                || StringUtils.isBlank(user.getGenderCode())
                || StringUtils.isBlank(user.getPhoneNumber());

        // 복호화 로직은 그대로 유지
        UserDto decryptedUser = UserDto.builder()
                .userId(user.getUserId())
                .userName(encryptionService.decrypt(user.getUserName()))
                .email(encryptionService.decrypt(user.getEmail()))
                .phoneNumber(encryptionService.decrypt(user.getPhoneNumber()))
                .birthDate(encryptionService.decrypt(user.getBirthDate()))
                .genderCode(user.getGenderCode())
                .build();

        List<String> roles = authMapper.findRoleByUserId(request.getUserId());
        log.info("사용자 '{}'의 권한 조회 결과: {}", request.getUserId(), roles);

        String accessToken = jwtTokenProvider.createToken(decryptedUser.getUserId(), roles, JwtTokenProvider.ACCESS_TOKEN_VALIDITY);
        String refreshToken = jwtTokenProvider.createToken(decryptedUser.getUserId(), List.of("ROLE_REFRESH"), JwtTokenProvider.REFRESH_TOKEN_VALIDITY);

        return LoginResponseDto.builder()
                .success(true)
                .message("로그인 성공")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .additionalInfoRequired(additionalInfoRequired)
                .build();
    }

    /**
     * 아이디 중복체크
     *
     * @param request 확인할 사용자 ID가 포함된 회원가입 요청 DTO
     * @return 중복된 아이디가 존재하면 true, 아니면 false
     */
    public boolean isDuplicateId(SignUpDto request) {
        return authMapper.existsById(request) > 0;
    }

    /**
     * 이메일 중복 체크
     * @param request 확인할 이메일이 포함된 회원가입 요청 DTO
     * @return 중복된 이메일이 존재하면 true, 아니면 false
     */
    public boolean isDuplicateEmail(SignUpDto request) {
        request.setEmailHash(encryptionService.hashWithSalt(request.getEmail()));
        return authMapper.existsByEmail(request) > 0;
    }

    /**
     * 회원가입
     * @param request 회원가입 정보를 담은 요청 DTO
     * @throws Exception 회원가입 처리 중 발생하는 예외
     */
    @Transactional
    public void signup(SignUpDto request) throws Exception {
        // 1. 비밀번호 복호화
        String decryptedPassword = rsaController.decrypt(request.getPassword());

        // 2. 유출된 비밀번호인지 서버 측에서 최종 확인
        boolean isPwned = Boolean.TRUE.equals(pwnedPasswordService.isPasswordPwned(decryptedPassword).block()); // 비동기 결과를 동기적으로 기다림
        if (isPwned) {
            throw new BsCoreException(
                    HttpStatusCode.BAD_REQUEST,
                    ErrorCode.WEAK_PASSWORD,
                    "이 비밀번호는 유출된 이력이 있어 사용할 수 없습니다. 다른 비밀번호를 사용해주세요.");
        }

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

        // 회원가입 및 권한 저장
        authMapper.signUpUser(item);
        authMapper.signUpUserRole(item);
    }

    /**
     * 아이디/비밀번호 찾기 서비스
     *
     * @param request 이메일 주소 등 사용자 정보를 담은 요청 DTO
     */
    public void searchIdBySendMail(UserInfoSearchDto request) {

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
            if(result == null || !encryptionService.decrypt(result.getUserName()).equals(request.getUserName()) || !encryptionService.decrypt(result.getEmail()).equals(request.getEmail()))
                throw new BsCoreException(
                        HttpStatusCode.INTERNAL_SERVER_ERROR
                        , ErrorCode.INVALID_INPUT
                        , "입력하신 정보와 일치하는 사용자가 없습니다.");

        } // PW

        /* 비정상 접근 계정 풀기 */
        if("ABNORMAL".equals(type)) {
            // 존재(Existence) 검증
            UserInfoSearchDto input = UserInfoSearchDto.builder()
                    .email(encryptionService.hashWithSalt(request.getEmail()))
                    .build();

            UserInfoSearchDto result = authMapper.findUserByNameMail(input);
            if(result == null || !encryptionService.decrypt(result.getUserName()).equals(request.getUserName()))
                throw new BsCoreException(
                        HttpStatusCode.INTERNAL_SERVER_ERROR
                        , ErrorCode.INVALID_INPUT
                        , "입력하신 정보와 일치하는 사용자가 없습니다.");
        }

        /* 휴먼계정 풀기 */
        if("DORMANT".equals(type)) {
            // 존재(Existence) 검증
            UserInfoSearchDto input = UserInfoSearchDto.builder()
                    .email(encryptionService.hashWithSalt(request.getEmail()))
                    .build();

            UserInfoSearchDto result = authMapper.findDormantUserByNameAndEmail(input);
            if(result == null || !encryptionService.decrypt(result.getUserName()).equals(request.getUserName()))
                throw new BsCoreException(
                        HttpStatusCode.INTERNAL_SERVER_ERROR
                        , ErrorCode.INVALID_INPUT
                        , "입력하신 정보와 일치하는 사용자가 없습니다.");
        }

        // 메일 전송
        String authCode = mailService.sendVerificationEmail(request.getEmail(), request.getUserName());

        // redis에 저장
        String key = VERIFICATION_PREFIX + request.getEmail();
        redisTemplate.opsForValue().set(key, authCode, Duration.ofMinutes(EXPIRATION_MINUTES));

    }

    /**
     * 인증코드 인증
     *
     * @param email 이메일
     * @param code 인증코드
     * @return 사용자 ID, 토큰
     */
    public UserInfoSearchDto verifyCode(String email, String code, String type) {

        String key = VERIFICATION_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        String userId = "";

        if (storedCode != null && storedCode.equals(code)) {
            // 인증 성공 시, 즉시 코드를 삭제하여 재사용을 방지.
            redisTemplate.delete(key);

            // 유저 조회
            UserInfoSearchDto input = UserInfoSearchDto.builder()
                    .email(encryptionService.hashWithSalt(email))
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 타입 체크
            if(StringUtils.isEmpty(type)) {
                throw new BsCoreException(
                        HttpStatusCode.INTERNAL_SERVER_ERROR
                        , ErrorCode.INVALID_INPUT
                        , "인증타입이 설정되지 않았습니다.");
            }

            // 아이디찾기, 비밀번호찾기, 비정상 접근
            if (type.matches("FIND_ID|FIND_PW|CHANGE_PW|ABNORMAL")) {
                // 이메일과 일치하는 정보 조회 후
                userId = authMapper.findUserIdByMail(input);
                input.setUserId(userId);
            }

            // DORMANT - 계정해제인 경우
            if (type.equals("DORMANT")) {
                // 이메일과 일치하는 정보를 휴먼 계정 테이블에서 조회
                UserInfoSearchDto result = authMapper.findDormantUserByNameAndEmail(input);
                userId = result.getUserId();
                input.setUserId(userId);
            }

            // 계정잠김 해제
            authMapper.updateUnLocked(input);

            return UserInfoSearchDto.builder()
                    .userId(userId)
                    // 만료시간 15분 -> 비밀번호 찾기 -> 비밀번호 변경에서 사용할 토큰
                    .token(jwtTokenProvider.createToken(userId, List.of("ROLE_REFRESH"), JwtTokenProvider.ACCESS_TOKEN_VALIDITY))
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
     * @param request 이메일 정보를 담은 요청 DTO
     * @return 조회된 사용자 아이디
     */
    public String searchIdByMail(UserInfoSearchDto request) {
        UserInfoSearchDto input = UserInfoSearchDto.builder()
                .email(encryptionService.hashWithSalt(request.getEmail()))
                .build();
        return authMapper.findUserIdByMail(input);
    }

    /**
     * 사용자 아이디로 보안질문 조회
     * @param request 유저 ID
     * @return 질문텍스트
     */
    public String searchPasswordHintById(UserInfoSearchDto request) {
        return authMapper.findPasswordHintById(request);
    }

    /**
     * 보안질문 정답 조회
     * @param request 유저 ID
     * @return 정답 결과
     */
    public UserInfoSearchDto searchHintAnswerById(UserInfoSearchDto request) {
        String passwordHintAnswer = authMapper.findHintAnswerById(request);
        if(StringUtils.isEmpty(passwordHintAnswer))
            throw new BsCoreException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR
                    , ErrorCode.INTERNAL_SERVER_ERROR
                    , "답변이 올바르지 않습니다.");
        if(encryptionService.decrypt(passwordHintAnswer).equals(request.getPasswordHintAnswer()))
            return UserInfoSearchDto.builder()
                    .userId(request.getUserId())
                    // 만료시간 15분 -> 비밀번호 찾기 -> 비밀번호 변경에서 사용할 토큰
                    .token(jwtTokenProvider.createToken(request.getUserId(), List.of("ROLE_REFRESH"), JwtTokenProvider.ACCESS_TOKEN_VALIDITY))
                    .build();
        else
            throw new BsCoreException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR
                    , ErrorCode.INTERNAL_SERVER_ERROR
                    , "답변이 올바르지 않습니다.");
    }

    /**
     * 비밀번호 업데이트
     *
     * @param token 인증 토큰
     * @param newPassword 새로운 비밀번호
     * @throws Exception 토큰 검증 실패 또는 비밀번호 업데이트 실패 시
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
                .passwordUpdated(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .updatedAt(LocalDateTime.now())
                .build();

        // 5. 비밀번호 업데이트
        authMapper.updatePassword(updateInput);

    }

}
