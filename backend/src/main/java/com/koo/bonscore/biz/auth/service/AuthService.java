package com.koo.bonscore.biz.auth.service;

import java.util.ArrayList;
import com.koo.bonscore.biz.auth.controller.RSAController;
import com.koo.bonscore.biz.auth.dto.LoginHistoryDto;
import com.koo.bonscore.biz.auth.dto.UserDto;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.auth.dto.req.UserInfoSearchDto;
import com.koo.bonscore.biz.auth.dto.res.LoginResponseDto;
import com.koo.bonscore.biz.auth.entity.LoginHistory;
import com.koo.bonscore.biz.authorization.entity.Role;
import com.koo.bonscore.biz.authorization.entity.RoleUser;
import com.koo.bonscore.biz.auth.entity.User;
import com.koo.bonscore.biz.users.entity.UserDormantInfo;
import com.koo.bonscore.biz.auth.repository.LoginHistoryRepository;
import com.koo.bonscore.biz.auth.repository.UserRepository;
import com.koo.bonscore.biz.authorization.repository.UserRoleRepository;
import com.koo.bonscore.biz.users.repository.UserDormantRepository;
import com.koo.bonscore.common.api.mail.service.MailService;
import com.koo.bonscore.core.config.enc.EncryptionService;
import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
@Transactional(readOnly = true)
public class AuthService {

    // JPA Repository
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserDormantRepository userDormantRepository;

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
        User user = validateCredentials(request.getUserId(), decryptedPassword);


        // 2. 휴면 계정 여부 확인
        if ("Y".equals(user.getAccountLocked())) {
            return createDormantAccountResponse();
        }

        // 3. 비정상 로그인 탐지
        boolean isAbnormal = checkAbnormalLogin(user, clientInfo);
        if (isAbnormal) {
            // 비정상 로그인 시, 추가 인증 요구 응답 반환
            return createAbnormalLoginResponse(request.getUserId());
        }

        // 4. 로그인 성공 처리
        processLoginSuccess(user, clientInfo);

        // 5. 토큰 발급 및 최종 응답 생성
        return createSuccessResponse(user);
    }

    /**
     * 1. 사용자 자격 증명을 확인
     */
    private User validateCredentials(String userId, String decryptedPassword) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BsCoreException(ErrorCode.INVALID_CREDENTIALS));

        if("Y".equals(user.getWithdrawn())) {
            throw new BsCoreException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
            throw new BsCoreException(ErrorCode.INVALID_CREDENTIALS);
        }

        return user;
    }

    /**
     * 2-1. 휴먼계정 응답객체 생성
     */
    private LoginResponseDto createDormantAccountResponse() {
        return LoginResponseDto.builder()
                .success(false)
                .reason("DORMANT_ACCOUNT")
                .code(ErrorCode.ACCOUNT_DORMANT_LONG_INACTIVE.getCode())
                .message("장기간 미접속으로 계정이 휴면 상태로 전환되었습니다.<br>본인인증을 통해 계정을 활성화할 수 있습니다.")
                .build();
    }

    /**
     * 3. 비정상 로그인 탐지
     */
    private boolean checkAbnormalLogin(User user, LoginHistoryDto clientInfo) {
        try {
            // 비정상 로그인으로 계정이 잠겨있는지 확인
            if("Y".equals(user.getRequiresVerificationYn())) {
                return true;
            }

            // Redis에서 최근 접속 국가 정보 조회
            String redisKey = "last_login_country:" + user.getUserId();
            String lastCountry = redisTemplate.opsForValue().get(redisKey);
            log.info("redisKey: {}, lastCountry: {}", redisKey, lastCountry);

            if (lastCountry != null && lastCountry.equals(clientInfo.getCountryCode())) {
                return false; // 최근 접속 국가와 동일하면 정상으로 간주
            }

            // Redis에 정보가 없거나 국가가 다르면 DB에서 전체 기록 조회
            List<String> recentCountries = loginHistoryRepository.findTop10ByUserIdOrderByLoginAtDesc(user.getUserId())
                    .stream()
                    .map(LoginHistory::getCountryCode) // 국가 코드만 추출
                    .distinct()                        // 중복 제거
                    .toList();                         // 리스트로 변환

            log.info("recentCountries: {}", recentCountries);
            log.info("clientInfo.getCountryCode(): {}", clientInfo.getCountryCode());

            if (recentCountries.isEmpty() || !recentCountries.contains(clientInfo.getCountryCode())) {
                // 계정 잠금 처리
                user.lockForVerification();
                log.warn("비정상 로그인 시도 감지: User ID = {}, IP = {}, Country = {}", user.getUserId(), clientInfo.getIpAddress(), clientInfo.getCountryCode());

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
    private void processLoginSuccess(User user, LoginHistoryDto clientInfo) {
        user.updateLoginTime();
        loginAttemptService.loginSucceeded(user.getUserId());
        saveLoginHistory(user.getUserId(), clientInfo);
    }

    /**
     * 4-1. 로그인 로그 저장
     */
    private void saveLoginHistory(String userId, LoginHistoryDto historyDto) {
        try {
            LoginHistory historyEntity = LoginHistory.builder()
                    .userId(userId)
                    .ipAddress(historyDto.getIpAddress())
                    .countryCode(historyDto.getCountryCode())
                    .loginAt(LocalDateTime.now())
                    .build();

            loginHistoryRepository.save(historyEntity);

            // Redis에 최근 접속 국가 정보 업데이트 (TTL 설정 가능)
            String redisKey = "last_login_country:" + userId;
            if (historyDto.getCountryCode() != null) {
                redisTemplate.opsForValue().set(redisKey, historyDto.getCountryCode(), Duration.ofDays(30));
            }

        } catch (Exception e) {
            log.error("로그인 기록 저장 중 오류 발생", e);
        }
    }

    /**
     * 6. 최종 성공 응답과 토큰을 생성
     */
    private LoginResponseDto createSuccessResponse(User user) {

        // Entity -> DTO 변환 및 추가 정보 필요 여부 확인
        boolean additionalInfoRequired = StringUtils.isBlank(user.getBirthDate())
                || StringUtils.isBlank(user.getGenderCode())
                || StringUtils.isBlank(user.getPhoneNumber());

        // 권한 조회
        List<String> roles = getRoles(user.getUserId());

        String accessToken = jwtTokenProvider.createToken(user.getUserId(), roles, JwtTokenProvider.ACCESS_TOKEN_VALIDITY);
        String refreshToken = jwtTokenProvider.createToken(user.getUserId(), List.of("ROLE_REFRESH"), JwtTokenProvider.REFRESH_TOKEN_VALIDITY);

        return LoginResponseDto.builder()
                .success(true)
                .message("로그인 성공")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .additionalInfoRequired(additionalInfoRequired)
                .build();
    }

    /**
     * 사용자 권한 목록 조회
     * @param userId 사용자 ID
     * @return 권한 목록 (예: ["USER", "ADMIN"])
     */
    @Transactional(readOnly = true)
    public List<String> getRoles(String userId) {
        return userRepository.findByUserId(userId)
                .map(user -> user.getRoles().stream()
                        .map(Role::getRoleId)
                        .collect(Collectors.toList()))
                .orElseGet(ArrayList::new);
    }

    /**
     * 아이디 중복체크
     *
     * @param request 확인할 사용자 ID가 포함된 회원가입 요청 DTO
     * @return 중복된 아이디가 존재하면 true, 아니면 false
     */
    public boolean isDuplicateId(SignUpDto request) {
        return userRepository.existsByUserId(request.getUserId());
    }

    /**
     * 이메일 중복 체크
     * @param request 확인할 이메일이 포함된 회원가입 요청 DTO
     * @return 중복된 이메일이 존재하면 true, 아니면 false
     */
    public boolean isDuplicateEmail(SignUpDto request) {
        String emailHash = encryptionService.hashWithSalt(request.getEmail());
        return userRepository.existsByEmailHash(emailHash);
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
            throw new BsCoreException(ErrorCode.WEAK_PASSWORD);
        }

        // 암호화 대상 : 유저명, 패스워드, 이메일, 전화번호, 생년월일
        User newUser = User.builder()
                .userId(request.getUserId())
                .userName(encryptionService.encrypt(request.getUserName()))
                .password(passwordEncoder.encode(rsaController.decrypt(request.getPassword())))
                .email(encryptionService.encrypt(request.getEmail()))
                .emailHash(encryptionService.hashWithSalt(request.getEmail()))
                .phoneNumber(encryptionService.encrypt(request.getPhoneNumber()))
                .birthDate(encryptionService.encrypt(request.getBirthDate()))
                .genderCode(request.getGenderCode())
                .passwordUpdated(LocalDateTime.now())
                .termsAgree1(request.getTermsAgree1())
                .termsAgree2(request.getTermsAgree2())
                .termsAgree3(request.getTermsAgree3())
                .termsAgree4(request.getTermsAgree4())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        RoleUser roleUser = RoleUser.builder()
                .userId(newUser.getUserId())
                .roleId("USER")
                .build();

        // 회원가입 및 권한 저장
        userRepository.save(newUser);
        userRoleRepository.save(roleUser);
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
            throw new BsCoreException(ErrorCode.USERNAME_REQUIRED);
        if(StringUtils.isEmpty(request.getEmail()))
            throw new BsCoreException(ErrorCode.EMAIL_REQUIRED);

        // 해싱된 이메일
        String emailHash = encryptionService.hashWithSalt(request.getEmail());

        /* 아이디 찾기 일 때 */
        if("ID".equals(type)) {

            // 존재(Existence) 검증
            User user = userRepository.findByEmailHashAndUserName(emailHash, request.getUserName())
                    .orElse(null);

            if(user == null) {
                // 이름까지 조건에 넣어서 못 찾았으면, 이메일로만 찾아서 복호화 후 비교
                user = userRepository.findByEmailHash(emailHash).orElse(null);
                if(user == null || !encryptionService.decrypt(user.getUserName()).equals(request.getUserName())) {
                    throw new BsCoreException(ErrorCode.USER_INFO_NOT_MATCH);
                }
            }

        } // ID
        
        /* 비밀번호 찾기 일 때 */
        if("PW".equals(type)) {

            // 형식(Format)검증
            if(StringUtils.isEmpty(request.getNonMaskedId()))
                throw new BsCoreException(ErrorCode.USERID_REQUIRED);

            User user = userRepository.findByUserId(request.getNonMaskedId()).orElse(null);
            if(user == null
                    || !encryptionService.decrypt(user.getUserName()).equals(request.getUserName())
                    || !encryptionService.decrypt(user.getEmail()).equals(request.getEmail())) {
                throw new BsCoreException(ErrorCode.USER_INFO_NOT_MATCH);
            }

        } // PW

        /* 비정상 접근 / 휴면 계정 풀기 */
        if("ABNORMAL".equals(type) || "DORMANT".equals(type)) {
            User user;
            if("DORMANT".equals(type)) {
                user = userRepository.findByEmailHashAndUserName(emailHash, request.getUserName()).orElse(null); // Native Query 사용
            } else {
                user = userRepository.findByEmailHash(emailHash).orElse(null);
            }

            if(user == null || !encryptionService.decrypt(user.getUserName()).equals(request.getUserName())) {
                throw new BsCoreException(ErrorCode.USER_INFO_NOT_MATCH);
            }
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
        String emailHash = encryptionService.hashWithSalt(email);

        if (storedCode != null && storedCode.equals(code)) {
            // 인증 성공 시, 즉시 코드를 삭제하여 재사용을 방지.
            redisTemplate.delete(key);

            // 타입 체크
            if(StringUtils.isEmpty(type))
                throw new BsCoreException(ErrorCode.AUTH_TYPE_NOT_SET);

            // 아이디찾기, 비밀번호찾기, 비정상 접근
            if (type.matches("FIND_ID|FIND_PW|PW|ABNORMAL")) {
                // 이메일과 일치하는 정보 조회 후
                User user = userRepository.findByEmailHash(emailHash)
                        .orElseThrow(() -> new BsCoreException(ErrorCode.USER_INFO_NOT_MATCH));
                userId = user.getUserId();
            }

            // DORMANT - 계정해제인 경우
            if (type.equals("DORMANT")) {
                UserDormantInfo dormantUser = userDormantRepository.findByEmailHash(emailHash)
                        .orElseThrow(() -> new BsCoreException(ErrorCode.USER_INFO_NOT_MATCH));
                userId = dormantUser.getUserId();
            }

            // 계정 잠금 해제 (User Entity 조회 후 상태 변경 -> Dirty Checking)
            userRepository.findByUserId(userId).ifPresent(User::unlockAccount);

            return UserInfoSearchDto.builder()
                    .userId(userId)
                    // 만료시간 15분 -> 비밀번호 찾기 -> 비밀번호 변경에서 사용할 토큰
                    .token(jwtTokenProvider.createToken(userId, List.of("ROLE_REFRESH"), JwtTokenProvider.ACCESS_TOKEN_VALIDITY))
                    .build();
        } else
            throw new BsCoreException(ErrorCode.AUTH_CODE_INVALID_OR_EXPIRED);
    }

    /**
     * 유저 ID 복사 시 호출
     * @param request 이메일 정보를 담은 요청 DTO
     * @return 조회된 사용자 아이디
     */
    public String searchIdByMail(UserInfoSearchDto request) {
        return userRepository.findByEmailHash(encryptionService.hashWithSalt(request.getEmail()))
                .map(User::getUserId)
                .orElse(null);
    }

    /**
     * 사용자 아이디로 보안질문 조회
     * @param request 유저 ID
     * @return 질문텍스트
     */
    public String searchPasswordHintById(UserInfoSearchDto request) {
        User user = userRepository.findByUserId(request.getUserId()).orElse(null);
        String question = "";
        assert user != null;
        if (user.getSecurityQuestion() != null) {
            question = user.getSecurityQuestion().getQuestionText();
        }
        return question;
    }

    /**
     * 보안질문 정답 조회
     * @param request 유저 ID
     * @return 정답 결과
     */
    public UserInfoSearchDto searchHintAnswerById(UserInfoSearchDto request) {
        User user = userRepository.findByUserId(request.getUserId()).orElseThrow(() -> new BsCoreException(ErrorCode.USER_INFO_NOT_MATCH));
        String passwordHintAnswer = user.getPasswordHintAnswer();
        if(StringUtils.isEmpty(passwordHintAnswer))
            throw new BsCoreException(ErrorCode.INCORRECT_ANSWER);
        if(encryptionService.decrypt(passwordHintAnswer).equals(request.getPasswordHintAnswer()))
            return UserInfoSearchDto.builder()
                    .userId(request.getUserId())
                    // 만료시간 15분 -> 비밀번호 찾기 -> 비밀번호 변경에서 사용할 토큰
                    .token(jwtTokenProvider.createToken(request.getUserId(), List.of("ROLE_REFRESH"), JwtTokenProvider.ACCESS_TOKEN_VALIDITY))
                    .build();
        else
            throw new BsCoreException(ErrorCode.INCORRECT_ANSWER);
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
            throw new BsCoreException(ErrorCode.TOKEN_INVALID_OR_EXPIRED);
        }

        String userId = jwtTokenProvider.getUserId(token);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BsCoreException(ErrorCode.INVALID_INPUT));

        String encodedPassword = passwordEncoder.encode(rsaController.decrypt(newPassword));

        // JPA Dirty Checking: 객체 상태만 변경하면 트랜잭션 종료 시 update 쿼리 실행됨
        user.changePassword(encodedPassword);
    }

}
