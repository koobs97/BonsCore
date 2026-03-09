package com.koo.bonscore.biz.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.res.LoginResponseDto;
import com.koo.bonscore.biz.auth.service.*;
import com.koo.bonscore.core.config.web.WebConfig;
import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.config.web.security.config.LoginSessionManager;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.log.service.UserActivityLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 슬라이스 테스트
 *
 * @WebMvcTest: 웹 레이어(Controller, Filter, ControllerAdvice)만 로드
 *              Service, Repository 등은 로드되지 않으므로 @MockBean으로 대체
 *
 * excludeAutoConfiguration: Spring Security 자동 설정을 제외하여
 *              인증 없이 모든 엔드포인트에 접근 가능하게 만든다.
 *
 * MockMvc: 실제 서버 없이 HTTP 요청/응답을 시뮬레이션하는 테스트 도구
 */
@WebMvcTest(
        value = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청/응답 시뮬레이터

    @Autowired
    private ObjectMapper objectMapper; // 객체 → JSON 변환

    // AuthController가 의존하는 빈들을 Mock으로 대체
    @MockitoBean private RSAController rsaController;
    @MockitoBean private AuthService authService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private LoginSessionManager loginSessionManager;
    @MockitoBean private GeoIpLocationService geoIpLocationService;
    @MockitoBean private PwnedPasswordService pwnedPasswordService;
    @MockitoBean private LoginAttemptService loginAttemptService;
    @MockitoBean private RecaptchaService recaptchaService;
    @MockitoBean private UserActivityLogService userActivityLogService;

    private static final String LOGIN_URL = "/api/auth/login";

    // ===== 입력값 유효성 검증 =====

    @Nested
    @DisplayName("POST /api/auth/login - 입력값 유효성 검증")
    class LoginValidationTest {

        @Test
        @DisplayName("userId가 빈 문자열이면 400 Bad Request를 반환한다")
        void login_WhenUserIdIsBlank_Returns400() throws Exception {
            // given - userId가 비어있는 요청
            LoginDto request = LoginDto.builder()
                    .userId("")
                    .password("encryptedPassword")
                    .build();

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print()) // 요청/응답 내용을 콘솔에 출력 (디버깅 용도)
                    .andExpect(status().isBadRequest()); // 400
        }

        @Test
        @DisplayName("password가 빈 문자열이면 400 Bad Request를 반환한다")
        void login_WhenPasswordIsBlank_Returns400() throws Exception {
            // given - password가 비어있는 요청
            LoginDto request = LoginDto.builder()
                    .userId("testUser")
                    .password("")
                    .build();

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest()); // 400
        }
    }

    // ===== 비즈니스 로직 검증 =====

    @Nested
    @DisplayName("POST /api/auth/login - 비즈니스 로직")
    class LoginBusinessTest {

        @Test
        @DisplayName("계정이 잠긴 상태로 로그인 시도 시 4xx 에러가 반환된다")
        void login_WhenAccountLocked_Returns4xxError() throws Exception {
            // given - 5회 초과 실패로 계정이 잠긴 상태
            String lockedUserId = "lockedUser";
            given(loginAttemptService.isAccountLocked(lockedUserId)).willReturn(true);

            LoginDto request = LoginDto.builder()
                    .userId(lockedUserId)
                    .password("anyPassword")
                    .build();

            // when & then
            // AuthController는 계정 잠금 시 BsCoreException(ACCOUNT_LOCKED)을 던짐
            // GlobalExceptionHandler(@ControllerAdvice)가 이를 받아 에러 응답으로 변환
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().is4xxClientError()); // 4xx (정확한 코드는 ErrorCode 설정에 따름)
        }

        @Test
        @DisplayName("로그인 성공 시 accessToken이 포함된 200 응답을 반환한다")
        void login_WhenCredentialsValid_Returns200WithToken() throws Exception {
            // given
            String userId = "testUser";

            // 계정 잠금/캡챠 불필요 설정
            given(loginAttemptService.isAccountLocked(userId)).willReturn(false);
            given(loginAttemptService.isCaptchaRequired(userId)).willReturn(false);

            // GeoIP 조회를 null 처리 (getClientInfo 내부에서 안전하게 처리됨)
            given(geoIpLocationService.getLocation(anyString())).willReturn(null);

            // 로그인 성공 응답 Mock
            LoginResponseDto successResponse = LoginResponseDto.builder()
                    .success(true)
                    .accessToken("mock.jwt.token")
                    .message("로그인 성공")
                    .build();
            given(authService.login(any(), any())).willReturn(successResponse);

            // 중복 로그인 없음
            given(loginSessionManager.isDuplicateLogin(userId)).willReturn(false);

            LoginDto request = LoginDto.builder()
                    .userId(userId)
                    .password("encryptedPassword")
                    .build();

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk()) // 200
                    .andExpect(jsonPath("$.data.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("mock.jwt.token"));
        }
    }
}
