package com.koo.bonscore.biz.users.service;

import com.koo.bonscore.biz.auth.service.AuthService;
import com.koo.bonscore.biz.users.dto.req.DormantUserInfoDto;
import com.koo.bonscore.biz.users.dto.res.UserInfoDto;
import com.koo.bonscore.biz.users.entity.User;
import com.koo.bonscore.biz.users.entity.UserDormantInfo;
import com.koo.bonscore.biz.users.repository.UserDormantRepository;
import com.koo.bonscore.biz.users.repository.UserRepository;
import com.koo.bonscore.core.config.enc.EncryptionService;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

/**
 * UserService 단위 테스트
 *
 * @ExtendWith(MockitoExtension.class): JUnit 5에서 Mockito를 활성화
 * @InjectMocks: 테스트 대상 객체 생성 + @Mock으로 선언한 의존성 자동 주입
 * @Mock: 가짜(Mock) 객체 생성 — 실제 DB나 외부 서비스를 호출하지 않음
 *
 * BDDMockito 스타일:
 *  given(...).willReturn(...)  → 특정 메서드 호출 시 반환값 지정
 *  any(Type.class)             → 어떤 값이든 매칭
 *  verify(mock).method(...)    → 해당 메서드가 실제로 호출됐는지 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private UserDormantRepository userDormantRepository;
    @Mock private EncryptionService encryptionService;
    @Mock private AuthService authService;

    // ===== 사용자 정보 조회 =====

    @Nested
    @DisplayName("사용자 정보 조회 - getUserInfo()")
    class GetUserInfoTest {

        @Test
        @DisplayName("존재하는 사용자 조회 시 복호화된 UserInfoDto를 반환한다")
        void getUserInfo_WhenUserExists_ReturnsDecryptedDto() {
            // given
            String userId = "testUser";

            User user = User.builder()
                    .userId(userId)
                    .userName("암호화된이름")
                    .email("암호화된이메일")
                    .phoneNumber("암호화된전화번호")
                    .birthDate("암호화된생년월일")
                    .genderCode("M")
                    .password("hash")
                    .build();

            given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
            given(authService.getRoles(userId)).willReturn(List.of("USER"));
            given(encryptionService.decrypt("암호화된이름")).willReturn("홍길동");
            given(encryptionService.decrypt("암호화된이메일")).willReturn("hong@test.com");
            given(encryptionService.decrypt("암호화된생년월일")).willReturn("19900101");
            given(encryptionService.decrypt("암호화된전화번호")).willReturn("01012345678");

            // when
            UserInfoDto result = userService.getUserInfo(userId);

            // then
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getUserName()).isEqualTo("홍길동");
            assertThat(result.getEmail()).isEqualTo("hong@test.com");
            assertThat(result.getRoleId()).isEqualTo("USER");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 조회 시 BsCoreException이 발생한다")
        void getUserInfo_WhenUserNotFound_ThrowsBsCoreException() {
            // given
            String unknownUserId = "ghost";
            given(userRepository.findByUserId(unknownUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserInfo(unknownUserId))
                    .isInstanceOf(BsCoreException.class);
        }
    }

    // ===== 휴면 예정자 안내 =====

    @Nested
    @DisplayName("휴면 전환 예정자 조회 - processDormancyNotice()")
    class ProcessDormancyNoticeTest {

        @Test
        @DisplayName("휴면 전환 예정자가 없으면 빈 리스트를 반환한다")
        void processDormancyNotice_WhenNoTargetUsers_ReturnsEmptyList() {
            // given - 조건에 해당하는 사용자 없음
            given(userRepository.findByLastLoginAtBeforeAndAccountLocked(
                    any(LocalDateTime.class), eq("N")))
                    .willReturn(List.of());

            // when
            List<DormantUserInfoDto> result = userService.processDormancyNotice();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ===== 휴면 해제 =====

    @Nested
    @DisplayName("휴면 계정 활성화 - activateDormantUser()")
    class ActivateDormantUserTest {

        @Test
        @DisplayName("휴면 해제 성공 시 휴면 테이블 데이터가 삭제된다")
        void activateDormantUser_WhenDormantUserExists_DeletesDormantRecord() {
            // given
            String email = "test@test.com";
            String emailHash = "hashedEmail";
            String userId = "dormantUser";

            UserDormantInfo dormantInfo = UserDormantInfo.builder()
                    .userId(userId)
                    .userName("암호화된이름")
                    .email("암호화된이메일")
                    .emailHash(emailHash)
                    .phoneNumber("암호화된전화번호")
                    .birthDate("암호화된생년월일")
                    .genderCode("M")
                    .termsAgree1("Y").termsAgree2("Y").termsAgree3("Y").termsAgree4("N")
                    .build();

            User dormantUser = User.builder()
                    .userId(userId)
                    .userName("DORMANT_USER")
                    .password("hash")
                    .accountLocked("Y") // 휴면 상태
                    .build();

            given(encryptionService.hashWithSalt(email)).willReturn(emailHash);
            given(userDormantRepository.findByEmailHash(emailHash)).willReturn(Optional.of(dormantInfo));
            given(userRepository.findByUserId(userId)).willReturn(Optional.of(dormantUser));

            DormantUserInfoDto request = DormantUserInfoDto.builder().email(email).build();

            // when
            userService.activateDormantUser(request);

            // then - 휴면 정보 삭제가 1회 호출되었는지 검증
            then(userDormantRepository).should(times(1)).delete(dormantInfo);
        }
    }
}
