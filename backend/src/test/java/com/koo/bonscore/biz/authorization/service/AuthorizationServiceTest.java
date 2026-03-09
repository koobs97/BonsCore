package com.koo.bonscore.biz.authorization.service;

import com.koo.bonscore.biz.auth.controller.RSAController;
import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.UpdateUserDto;
import com.koo.bonscore.biz.authorization.dto.req.UserReqDto;
import com.koo.bonscore.biz.authorization.dto.res.MenuByRoleDto;
import com.koo.bonscore.biz.authorization.dto.res.UserResDto;
import com.koo.bonscore.biz.users.entity.User;
import com.koo.bonscore.biz.users.repository.SecurityQuestionRepository;
import com.koo.bonscore.biz.users.repository.UserRepository;
import com.koo.bonscore.core.config.enc.EncryptionService;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.log.repository.UserActivityLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

/**
 * AuthorizationService 단위 테스트
 *
 * 핵심 검증 대상
 *  - getMenuByRole()      : 사용자 미존재 시 예외, 정상 조회
 *  - getUserInfos()       : 빈 결과 반환
 *  - updateUserInfo()     : 사용자 미존재 시 예외, 정상 업데이트
 *  - passwordValidate()   : 일치/불일치 검증
 *  - updateWithdrawn()    : 사용자 미존재 시 예외, 탈퇴 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService")
class AuthorizationServiceTest {

    @InjectMocks
    private AuthorizationService authorizationService;

    @Mock private UserRepository userRepository;
    @Mock private UserActivityLogRepository userActivityLogRepository;
    @Mock private SecurityQuestionRepository securityQuestionRepository;
    @Mock private RSAController rsaController;
    @Mock private EncryptionService encryptionService;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    // ===== 권한별 메뉴 조회 =====

    @Nested
    @DisplayName("권한별 메뉴 조회 - getMenuByRole()")
    class GetMenuByRoleTest {

        @Test
        @DisplayName("존재하지 않는 사용자면 BsCoreException이 발생한다")
        void getMenuByRole_WhenUserNotFound_ThrowsBsCoreException() {
            // given
            given(userRepository.findByUserId(anyString())).willReturn(Optional.empty());
            AuthorizationDto request = new AuthorizationDto("ghost");

            // when & then
            assertThatThrownBy(() -> authorizationService.getMenuByRole(request))
                    .isInstanceOf(BsCoreException.class);
        }

        @Test
        @DisplayName("역할이 없는 사용자면 빈 메뉴 리스트를 반환한다")
        void getMenuByRole_WhenUserHasNoRoles_ReturnsEmptyList() {
            // given
            User user = User.builder()
                    .userId("testUser")
                    .password("hash")
                    .roles(new ArrayList<>())
                    .build();
            given(userRepository.findByUserId(anyString())).willReturn(Optional.of(user));
            AuthorizationDto request = new AuthorizationDto("testUser");

            // when
            List<MenuByRoleDto> result = authorizationService.getMenuByRole(request);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ===== 사용자 목록 조회 =====

    @Nested
    @DisplayName("사용자 목록 조회 - getUserInfos()")
    class GetUserInfosTest {

        @Test
        @DisplayName("검색 결과가 없으면 빈 리스트를 반환한다")
        void getUserInfos_WhenNoResult_ReturnsEmptyList() {
            // given - 조건에 맞는 사용자 없음
            given(userRepository.findUsersByCondition(any(), any(), any(), any()))
                    .willReturn(List.of());
            UserReqDto request = new UserReqDto();

            // when
            List<UserResDto> result = authorizationService.getUserInfos(request);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ===== 사용자 정보 수정 =====

    @Nested
    @DisplayName("사용자 정보 수정 - updateUserInfo()")
    class UpdateUserInfoTest {

        @Test
        @DisplayName("존재하지 않는 사용자면 BsCoreException이 발생한다")
        void updateUserInfo_WhenUserNotFound_ThrowsBsCoreException() {
            // given
            given(userRepository.findByUserId(anyString())).willReturn(Optional.empty());
            UpdateUserDto request = UpdateUserDto.builder().userId("ghost").build();

            // when & then
            assertThatThrownBy(() -> authorizationService.updateUserInfo(request))
                    .isInstanceOf(BsCoreException.class);
        }

        @Test
        @DisplayName("유효한 사용자면 암호화된 정보로 업데이트된다")
        void updateUserInfo_WhenUserFound_EncryptsAndUpdates() {
            // given
            User user = User.builder().userId("testUser").password("hash").build();
            given(userRepository.findByUserId(anyString())).willReturn(Optional.of(user));
            given(encryptionService.encrypt(anyString())).willReturn("encrypted");
            given(encryptionService.hashWithSalt(anyString())).willReturn("hashedEmail");

            UpdateUserDto request = UpdateUserDto.builder()
                    .userId("testUser")
                    .userName("홍길동")
                    .email("test@test.com")
                    .phoneNumber("01012345678")
                    .birthDate("19900101")
                    .genderCode("M")
                    .build();

            // when - 예외 없이 완료되면 성공 (JPA Dirty Checking)
            authorizationService.updateUserInfo(request);

            // then
            then(encryptionService).should(atLeastOnce()).encrypt(anyString());
        }
    }

    // ===== 비밀번호 검증 =====

    @Nested
    @DisplayName("비밀번호 검증 - passwordValidate()")
    class PasswordValidateTest {

        @Test
        @DisplayName("비밀번호가 일치하면 true를 반환한다")
        void passwordValidate_WhenPasswordMatches_ReturnsTrue() throws Exception {
            // given
            User user = User.builder().userId("testUser").password("bcryptHash").build();
            given(userRepository.findByUserId(anyString())).willReturn(Optional.of(user));
            given(rsaController.decrypt(anyString())).willReturn("plainPassword");
            given(passwordEncoder.matches("plainPassword", "bcryptHash")).willReturn(true);

            UpdateUserDto request = UpdateUserDto.builder()
                    .userId("testUser").password("encPw").build();

            // when
            boolean result = authorizationService.passwordValidate(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("비밀번호가 불일치하면 false를 반환한다")
        void passwordValidate_WhenPasswordNotMatches_ReturnsFalse() throws Exception {
            // given
            User user = User.builder().userId("testUser").password("bcryptHash").build();
            given(userRepository.findByUserId(anyString())).willReturn(Optional.of(user));
            given(rsaController.decrypt(anyString())).willReturn("wrongPassword");
            given(passwordEncoder.matches("wrongPassword", "bcryptHash")).willReturn(false);

            UpdateUserDto request = UpdateUserDto.builder()
                    .userId("testUser").password("encPw").build();

            // when
            boolean result = authorizationService.passwordValidate(request);

            // then
            assertThat(result).isFalse();
        }
    }

    // ===== 회원 탈퇴 =====

    @Nested
    @DisplayName("회원 탈퇴 - updateWithdrawn()")
    class UpdateWithdrawnTest {

        @Test
        @DisplayName("존재하지 않는 사용자면 BsCoreException이 발생한다")
        void updateWithdrawn_WhenUserNotFound_ThrowsBsCoreException() {
            // given
            given(userRepository.findByUserId(anyString())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authorizationService.updateWithdrawn("ghost"))
                    .isInstanceOf(BsCoreException.class);
        }

        @Test
        @DisplayName("유효한 사용자면 withdrawn=Y로 설정되고 PII가 파기된다")
        void updateWithdrawn_WhenUserFound_WithdrawsUser() {
            // given
            User user = User.builder()
                    .userId("testUser")
                    .userName("암호화된이름")
                    .email("암호화된이메일")
                    .password("bcryptHash")
                    .build();
            given(userRepository.findByUserId("testUser")).willReturn(Optional.of(user));

            // when
            authorizationService.updateWithdrawn("testUser");

            // then - Entity의 withdrawUser() 호출 결과 검증 (Dirty Checking)
            assertThat(user.getWithdrawn()).isEqualTo("Y");
            assertThat(user.getEmail()).isNull();
        }
    }
}
