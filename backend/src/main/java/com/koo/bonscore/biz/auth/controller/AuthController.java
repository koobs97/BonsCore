package com.koo.bonscore.biz.auth.controller;

import com.koo.bonscore.biz.auth.dto.LoginHistoryDto;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.auth.dto.req.UserInfoSearchDto;
import com.koo.bonscore.biz.auth.dto.res.LoginResponseDto;
import com.koo.bonscore.biz.auth.dto.res.RefreshTokenDto;
import com.koo.bonscore.biz.auth.mapper.AuthMapper;
import com.koo.bonscore.biz.auth.service.*;
import com.koo.bonscore.common.util.web.WebUtils;
import com.koo.bonscore.core.annotaion.PreventDoubleClick;
import com.koo.bonscore.core.config.api.ApiResponse;
import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.config.web.security.config.LoginSessionManager;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
import com.koo.bonscore.core.exception.response.ErrorResponse;
import com.koo.bonscore.log.annotaion.UserActivityLog;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static com.koo.bonscore.core.exception.enumType.ErrorCode.INVALID_CREDENTIALS;

/**
 * <pre>
 * AuthController.java
 * 설명 : 인증
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-01
 */
@Slf4j
@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthMapper authMapper;

    private final RSAController rsaController;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginSessionManager loginSessionManager;
    private final GeoIpLocationService geoIpLocationService;
    private final PwnedPasswordService pwnedPasswordService;

    private final LoginAttemptService loginAttemptService;
    private final RecaptchaService recaptchaService;


    /**
     * 로그인
     * - 사용자 로그인을 처리하고, 성공 시 Access Token과 Refresh Token을 발급
     *
     * @param request 로그인에 필요한 사용자 ID와 비밀번호를 담은 DTO
     * @param httpRequest 사용자 활동 로그(Activity Log)에 실패 결과를 기록하기 위해 사용되는 HttpServletRequest 객체
     * @param httpResponse 로그인 성공 시 Refresh Token을 쿠키에 담아 전달하기 위한 HttpServletResponse 객체
     * @return 로그인 성공 여부, Access Token, 메시지 등을 포함하는 응답 DTO (LoginResponseDto)
     * @throws Exception 로그인 과정에서 발생하는 모든 예외
     */
    @UserActivityLog(activityType = "LOGIN", userIdField = "#request.userId")
    @PreventDoubleClick
    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {

        String userId = request.getUserId();

        // 계정이 5회 초과 잠김인지 확인
        if (loginAttemptService.isAccountLocked(request.getUserId())) {
            throw new BsCoreException(
                    HttpStatusCode.INTERNAL_SERVER_ERROR,
                    ErrorCode.ACCOUNT_LOCKED,
                    "최대 로그인 횟수(5회)를 초과하여 계정이 잠겼습니다. 5분 후에 다시 시도해주세요.");
        }

        // CAPTCHA 검증이 필요한지 확인
        if (loginAttemptService.isCaptchaRequired(userId)) {
            String recaptchaToken = request.getRecaptchaToken();

            if (recaptchaToken == null || recaptchaToken.isBlank()) {
                return LoginResponseDto.builder()
                        .success(false)
                        .message("계정 보안을 위해 reCAPTCHA 인증이 필요합니다.")
                        .captchaRequired(true)
                        .build();
            }

            boolean isRecaptchaValid = Boolean
                    .TRUE
                    .equals(recaptchaService.verifyRecaptcha(recaptchaToken).block());
            if (!isRecaptchaValid) {
                return LoginResponseDto.builder()
                        .success(false)
                        .message("자동 입력 방지 문자 확인에 실패했습니다.")
                        .captchaRequired(true)
                        .build();
            }
        }

        /* 로그인 인증 시작 */
        try {

            // 클라이언트 정보 추출
            LoginHistoryDto clientInfo = getClientInfo(request, httpRequest);

            // 실제 로그인 처리
            LoginResponseDto responseDto = authService.login(request, clientInfo);

            // 로그인 성공 시 세션 처리
            if (responseDto.getSuccess()) {

                // 사용자 ID로 중복 로그인 확인
                if (!request.isForce() && loginSessionManager.isDuplicateLogin(request.getUserId())) {
                    responseDto.setSuccess(false);
                    responseDto.setReason("DUPLICATE_LOGIN");
                    responseDto.setMessage("다른 기기에서 로그인 중입니다.<br>접속을 강제로 끊고 로그인하시겠습니까?");
                    return responseDto;
                }

                String accessToken = responseDto.getAccessToken();

                // 새로운 세션 등록 (이 과정에서 기존 세션은 블랙리스트 처리됨
                loginSessionManager.registerSession(userId, accessToken);

                // Refresh Token 쿠키로 전달
                ResponseCookie cookie = ResponseCookie.from("refresh_token", responseDto.getRefreshToken())
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Strict")
                        .path("/")
                        .maxAge(JwtTokenProvider.REFRESH_TOKEN_VALIDITY / 1000)
                        .build();

                httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

                // 보안상 응답 body에는 refreshToken 제거
                responseDto.setRefreshToken(null);
            }

            return responseDto;

        } catch (BsCoreException e) {

            // 활동 로그를 위한 속성 설정
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());

            // 실패 기록
            loginAttemptService.loginFailed(userId);

            // reCAPTCHA가 필요하다면, 예외 메시지 대신 reCAPTCHA 안내 메시지를 사용
            boolean captchaRequired = loginAttemptService.isCaptchaRequired(userId);
            String responseMessage = captchaRequired
                    ? "계정 보안을 위해 reCAPTCHA 인증이 필요합니다."
                    : e.getMessage();

            // 실패 후 응답 DTO 생성
            return LoginResponseDto.builder()
                    .success(false)
                    .message(responseMessage)
                    .captchaRequired(captchaRequired)
                    .build();

        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 클라이언트 접속정보 추출
     *
     * @param request 로그인 시도 정보
     * @param httpRequest HttpServletRequest
     * @return 로그인 로그 기록 dto
     */
    private LoginHistoryDto getClientInfo(LoginDto request, HttpServletRequest httpRequest) {

        String ip = WebUtils.getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        CityResponse location = null;
        String country = "N/A"; // 기본값 설정
        String city = "Localhost"; // 기본값 설정

        // Localhost IP (IPv6, IPv4) 제외
        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            city = "Localhost";
        } else {
            try {
                location = geoIpLocationService.getLocation(ip);
            } catch (AddressNotFoundException e) {
                log.warn("GeoIP database does not contain the address: {}", ip);
            } catch (Exception e) {
                log.error("GeoIP location lookup failed unexpectedly for IP: {}", ip, e);
            }
        }

        if (location != null) {
            if (location.getCountry() != null) {
                country = location.getCountry().getIsoCode();
            }
            if (location.getCity() != null) {
                city = location.getCity().getName();
            }
        }

        return new LoginHistoryDto(request.getUserId(), ip, userAgent, country, city);
    }

    /**
     * Refresh Token 검증 및 Access Token 생성
     *
     * @param refreshTokenDto Refresh Token 값을 담고 있는 요청 DTO
     * @return 새로운 Access Token을 포함한 성공 응답, 또는 토큰이 유효하지 않을 경우 에러 응답
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Object>> refreshAccessToken(@RequestBody RefreshTokenDto refreshTokenDto) {
        String refreshToken = refreshTokenDto.getRefreshToken();

        // Refresh Token 검증
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            ErrorResponse errorResponse = new ErrorResponse(
                    LocalDateTime.now(),                                                            // timestamp : 발생시각
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),                                       // status: 기본 HTTP 상태 코드 (500)
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),                             // error: 기본 HTTP 상태 설명
                    ErrorCode.INVALID_REFRESH_TOKEN.getCode(),                                      // code: 사용자 정의 에러 코드
                    ErrorCode.INVALID_REFRESH_TOKEN.getMessage(),                                   // message: 예외 메시지
                    "/api/refresh" // path: 요청 경로
            );
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure(ErrorCode.INVALID_REFRESH_TOKEN.getCode(), "접속권한이 없습니다.", errorResponse));
        }

        // Refresh Token에서 사용자 정보 추출
        String userId = jwtTokenProvider.getUserId(refreshToken);

        // 유저 권한 조회
        List<String> roles = authMapper.findRoleByUserId(userId);

        // 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.createToken(userId, roles, JwtTokenProvider.ACCESS_TOKEN_VALIDITY);

        // 새로운 Access Token 반환
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", newAccessToken));
    }

    /**
     * 로그아웃
     * - 클라이언트의 Refresh Token 쿠키를 삭제하도록 응답 헤더를 설정
     *
     * @param request Authorization 헤더의 Access Token과 쿠키의 Refresh Token을 추출하기 위한 HTTP 요청 객체
     * @param response Refresh Token 쿠키 삭제 명령을 헤더에 추가하기 위한 HTTP 응답 객체
     * @return 로그아웃 성공 메시지를 포함한 공통 응답 객체
     */
    @UserActivityLog(activityType = "LOGOUT")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(HttpServletRequest request, HttpServletResponse response) {

        // 1. Request Header에서 Access Token 추출
        String accessToken = jwtTokenProvider.resolveToken(request);
        if (accessToken == null) {
            // 토큰이 없는 요청은 처리할 필요 없음
            return ResponseEntity.ok(ApiResponse.success("No active session to log out.", null));
        }

        // 2. Access Token에서 사용자 ID 추출
        String userId = jwtTokenProvider.getUserId(accessToken);

        // 3. 쿠키에서 Refresh Token 값 찾기 (HttpOnly 쿠키이므로 서버에서 직접 읽기)
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        // 4. LoginSessionManager를 통해 서버 측 세션 정보 및 토큰 무효화
        loginSessionManager.logoutSession(userId, accessToken, refreshToken);

        // 5. 클라이언트(브라우저)의 Refresh Token 쿠키를 삭제하라는 응답 생성
        ResponseCookie deleteCookie = ResponseCookie.from("refresh_token", "")
                .maxAge(0) // maxAge를 0으로 설정하여 즉시 만료
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Successfully logged out.", null));
    }

    /**
     * 아이디 중복 체크
     *
     * @param request 확인할 사용자 ID가 포함된 회원가입 요청 DTO
     * @return 중복된 아이디가 존재하면 true, 아니면 false
     */
    @PreventDoubleClick
    @PostMapping("/isDuplicateId")
    public boolean isDuplicateId(@RequestBody SignUpDto request, HttpServletRequest httpRequest) {
        try {
            return authService.isDuplicateId(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 이메일 중복체크(이메일 입력 blur 이벤트 시 호출)
     *
     * @param request 확인할 이메일이 포함된 회원가입 요청 DTO
     * @return 중복된 이메일이 존재하면 true, 아니면 false
     */
    @PreventDoubleClick
    @PostMapping("/isDuplicateEmail")
    public boolean isDuplicateEmail(@RequestBody SignUpDto request, HttpServletRequest httpRequest) {
        try {
            return authService.isDuplicateEmail(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 유출된 비밀번호인지 확인. (실시간 검증용)
     * @param request 비밀번호가 포함된 DTO
     * @return 유출된 경우 true, 아닌 경우 false
     */
    @PostMapping("/check-pwned-password")
    public Mono<Boolean> checkPwnedPassword(@RequestBody SignUpDto request, HttpServletRequest httpRequest) throws Exception {
        try {
            String decryptedPassword = rsaController.decrypt(request.getPassword());
            return pwnedPasswordService.isPasswordPwned(decryptedPassword);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 회원가입
     *
     * @param request 회원가입 정보를 담은 요청 DTO
     * @throws Exception 회원가입 처리 중 발생하는 예외
     */
    @UserActivityLog(activityType = "SIGNUP", userIdField = "#request.userId")
    @PreventDoubleClick
    @PostMapping("/signup")
    public void signup(@RequestBody SignUpDto request, HttpServletRequest httpRequest) throws Exception {
        try {
            authService.signup(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 인증 이메일 발송 서비스
     *
     * @param request 이메일 주소 등 사용자 정보를 담은 요청 DTO
     */
    @UserActivityLog(activityType = "SEND_MAIL", userIdField = "#request.email")
    @PreventDoubleClick
    @PostMapping("/sendmail")
    public void sendMail(@RequestBody UserInfoSearchDto request, HttpServletRequest httpRequest) {
        try {
            authService.searchIdBySendMail(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 이메일 인증번호 인증
     *
     * @param request 이메일과 사용자가 입력한 인증코드를 담은 요청 DTO
     * @return 인증 성공 시 다음 단계 진행을 위한 정보(예: 임시 토큰)를 담은 DTO
     */
    @UserActivityLog(activityType = "CHECK_CODE", userIdField = "#request.email")
    @PreventDoubleClick
    @PostMapping("/verify-email")
    public UserInfoSearchDto verifyEmail(@RequestBody UserInfoSearchDto request, HttpServletRequest httpRequest) {
        try {
            return authService.verifyCode(request.getEmail(), request.getCode(), request.getType());
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 아이디 찾기 완료 후 아이디 필드 복사 시 호출
     * 아이디 찾기 완료 후 클립보드 복사 등의 기능을 위해 사용자 아이디를 반환
     *
     * @param request 이메일 정보를 담은 요청 DTO
     * @param httpRequest 사용자 활동 로그에 실패 정보를 기록하기 위한 HTTP 요청 객체
     * @return 조회된 사용자 아이디
     */
    @UserActivityLog(activityType = "COPY_ID", userIdField = "#request.email")
    @PostMapping("/copy-id")
    public String searchIdByMail(@RequestBody UserInfoSearchDto request, HttpServletRequest httpRequest) {
        try {
            return authService.searchIdByMail(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 사용자 아이디로 보안질문 조회
     *
     * @param request 사용자 ID
     * @return 보안질문
     */
    @UserActivityLog(activityType = "GET_HINT", userIdField = "#request.userId")
    @PostMapping("/search-hint")
    public String searchPasswordHintById(@RequestBody UserInfoSearchDto request, HttpServletRequest httpRequest) {
        try {
            return authService.searchPasswordHintById(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 보안질문 정답 입력
     *
     * @param request 유저 ID
     * @return 유저 ID
     */
    @UserActivityLog(activityType = "VALIDATE_ANSWER", userIdField = "#request.userId")
    @PostMapping("/validate-answer")
    public UserInfoSearchDto searchHintAnswerById(@RequestBody UserInfoSearchDto request, HttpServletRequest httpRequest) {
        try {
            return authService.searchHintAnswerById(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 비밀번호 찾기 후 비밀번호 업데이트
     *
     * @param request 인증 토큰과 새로운 비밀번호를 담은 요청 DTO
     * @throws Exception 토큰 검증 실패 또는 비밀번호 업데이트 실패 시
     */
    @UserActivityLog(activityType = "UPDATE_PWD", userIdField = "#request.userId")
    @PostMapping("/update-password")
    public void updatePassowrd(@RequestBody UserInfoSearchDto request, HttpServletRequest httpRequest) throws Exception {
        try {
            authService.resetPasswordWithToken(request.getToken(), request.getPassword());
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

}
