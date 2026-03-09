package com.koo.bonscore.biz.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koo.bonscore.biz.users.dto.req.DormantUserInfoDto;
import com.koo.bonscore.biz.users.dto.res.UserInfoDto;
import com.koo.bonscore.biz.users.service.UserService;
import com.koo.bonscore.core.config.web.WebConfig;
import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.config.web.security.config.LoginSessionManager;
import com.koo.bonscore.log.service.UserActivityLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 테스트
 *
 * @AuthenticationPrincipal을 사용하는 메서드 → @ExtendWith(MockitoExtension.class) 직접 호출로 테스트
 * HTTP 레이어 테스트가 필요한 메서드 → @WebMvcTest 슬라이스 테스트 사용
 */
@DisplayName("UserController")
class UserControllerTest {

    // ===== 내 정보 조회 (@AuthenticationPrincipal 사용 → 직접 호출 방식) =====

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("GET /api/users/me - 사용자 정보 조회 (직접 호출)")
    class GetMeTest {

        @InjectMocks
        private UserController userController;

        @Mock
        private UserService userService;

        @Test
        @DisplayName("인증된 사용자로 조회 시 UserInfoDto를 반환한다")
        void getMe_WhenAuthenticated_ReturnsUserInfoDto() {
            // given
            UserInfoDto userInfo = UserInfoDto.builder()
                    .userId("testUser")
                    .userName("홍길동")
                    .email("hong@test.com")
                    .roleId("USER")
                    .build();
            given(userService.getUserInfo("testUser")).willReturn(userInfo);

            User mockPrincipal = new User("testUser", "", List.of());

            // when - 컨트롤러 메서드 직접 호출 (Spring 컨텍스트 불필요)
            UserInfoDto result = userController.loginSuccess(mockPrincipal);

            // then
            assertThat(result.getUserId()).isEqualTo("testUser");
            assertThat(result.getUserName()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("인증 정보가 null이면 IllegalStateException이 발생한다")
        void getMe_WhenUnauthenticated_ThrowsIllegalStateException() {
            // when & then - null UserDetails 전달 시 컨트롤러의 null-guard 동작 검증
            assertThatThrownBy(() -> userController.loginSuccess(null))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ===== 휴면 계정 활성화 (@WebMvcTest 슬라이스 테스트) =====

    @Nested
    @WebMvcTest(
            value = UserController.class,
            excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
    )
    @DisplayName("PATCH /api/users/me/dormant - 휴면 계정 활성화")
    class ActivateDormantTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean private UserService userService;
        @MockitoBean private UserActivityLogService userActivityLogService;
        @MockitoBean private JwtTokenProvider jwtTokenProvider;
        @MockitoBean private LoginSessionManager loginSessionManager;

        @Test
        @DisplayName("휴면 활성화 요청 시 서비스를 호출하고 200을 반환한다")
        void activateDormant_WhenValidRequest_Returns200() throws Exception {
            // given
            DormantUserInfoDto request = DormantUserInfoDto.builder()
                    .email("test@test.com")
                    .build();
            willDoNothing().given(userService).activateDormantUser(any(DormantUserInfoDto.class));

            // when & then
            mockMvc.perform(patch("/api/users/me/dormant")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }
}
