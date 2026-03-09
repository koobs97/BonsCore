package com.koo.bonscore.biz.authorization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.UpdateUserDto;
import com.koo.bonscore.biz.authorization.dto.res.*;
import com.koo.bonscore.biz.authorization.service.AuthorizationService;
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
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthorizationController 테스트
 *
 * @AuthenticationPrincipal을 사용하는 메서드 → @ExtendWith(MockitoExtension.class) 직접 호출로 테스트
 * HTTP 레이어 테스트가 필요한 메서드 → @WebMvcTest 슬라이스 테스트 사용
 */
@DisplayName("AuthorizationController")
class AuthorizationControllerTest {

    // ===== @AuthenticationPrincipal 사용 메서드 직접 호출 테스트 =====

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("@AuthenticationPrincipal 사용 메서드 (직접 호출)")
    class DirectCallTest {

        @InjectMocks
        private AuthorizationController authorizationController;

        @Mock
        private AuthorizationService authorizationService;

        @Test
        @DisplayName("getMenus() - 인증된 사용자의 메뉴를 반환한다")
        void getMenus_WhenAuthenticated_ReturnsMenuList() throws Exception {
            // given
            given(authorizationService.getMenuByRole(any(AuthorizationDto.class))).willReturn(List.of());
            User mockPrincipal = new User("testUser", "", List.of());

            // when
            List<MenuByRoleDto> result = authorizationController.getMenus(mockPrincipal, null, null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("updateWithdrawn() - 인증된 사용자의 탈퇴 처리가 호출된다")
        void updateWithdrawn_WhenAuthenticated_CallsService() throws Exception {
            // given
            User mockPrincipal = new User("testUser", "", List.of());

            // when
            authorizationController.updateWithdrawn(mockPrincipal, null, null);

            // then
            then(authorizationService).should(times(1)).updateWithdrawn("testUser");
        }
    }

    // ===== HTTP 레이어 테스트 (@WebMvcTest 슬라이스 테스트) =====

    @Nested
    @WebMvcTest(
            value = AuthorizationController.class,
            excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
    )
    @DisplayName("GET /api/authorization/users - 사용자 목록 조회")
    class GetUserInfosTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean private AuthorizationService authorizationService;
        @MockitoBean private UserActivityLogService userActivityLogService;
        @MockitoBean private JwtTokenProvider jwtTokenProvider;
        @MockitoBean private LoginSessionManager loginSessionManager;

        @Test
        @DisplayName("요청 시 200과 사용자 목록을 반환한다")
        void getUserInfos_Returns200WithUserList() throws Exception {
            // given
            given(authorizationService.getUserInfos(any())).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/authorization/users"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @WebMvcTest(
            value = AuthorizationController.class,
            excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
    )
    @DisplayName("PATCH /api/authorization/users - 사용자 정보 수정")
    class UpdateUserInfoTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean private AuthorizationService authorizationService;
        @MockitoBean private UserActivityLogService userActivityLogService;
        @MockitoBean private JwtTokenProvider jwtTokenProvider;
        @MockitoBean private LoginSessionManager loginSessionManager;

        @Test
        @DisplayName("유효한 요청 시 200을 반환한다")
        void updateUserInfo_WhenValidRequest_Returns200() throws Exception {
            // given - void 메서드는 별도 설정 불필요
            UpdateUserDto request = UpdateUserDto.builder()
                    .userId("testUser")
                    .userName("홍길동")
                    .email("test@test.com")
                    .phoneNumber("01012345678")
                    .birthDate("19900101")
                    .genderCode("M")
                    .build();

            // when & then
            mockMvc.perform(patch("/api/authorization/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());

            then(authorizationService).should(times(1)).updateUserInfo(any(UpdateUserDto.class));
        }
    }
}
