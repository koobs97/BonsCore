package com.koo.bonscore.biz.users.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User Entity 비즈니스 메서드 단위 테스트
 *
 * Spring Context 없이 순수 Java 레벨에서 Entity의 상태 변화를 검증한다.
 * 테스트 원칙: Given(준비) - When(실행) - Then(검증)
 */
@DisplayName("User 엔티티")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 공통으로 사용할 정상 User 객체를 생성
        user = User.builder()
                .userId("testUser")
                .userName("암호화된이름")
                .password("encodedPassword")
                .email("암호화된이메일")
                .phoneNumber("암호화된전화번호")
                .birthDate("암호화된생년월일")
                .genderCode("M")
                .build();
    }

    // ===== 계정 잠금/해제 =====

    @Nested
    @DisplayName("계정 잠금/해제")
    class AccountLockTest {

        @Test
        @DisplayName("비정상 로그인 감지 시 추가 인증 요구 상태(Y)로 변경된다")
        void lockForVerification_ShouldSetRequiresVerificationToY() {
            // given - setUp()에서 생성된 정상 계정 (requiresVerificationYn = "N")

            // when
            user.lockForVerification();

            // then
            assertThat(user.getRequiresVerificationYn()).isEqualTo("Y");
        }

        @Test
        @DisplayName("계정 잠금 해제 시 requiresVerificationYn, accountLocked 모두 N으로 초기화된다")
        void unlockAccount_ShouldResetAllLockFlagsToN() {
            // given - 추가 인증이 요구된 상태
            user.lockForVerification();
            assertThat(user.getRequiresVerificationYn()).isEqualTo("Y"); // 전제 확인

            // when
            user.unlockAccount();

            // then
            assertThat(user.getRequiresVerificationYn()).isEqualTo("N");
            assertThat(user.getAccountLocked()).isEqualTo("N");
        }
    }

    // ===== 회원 탈퇴 =====

    @Nested
    @DisplayName("회원 탈퇴")
    class WithdrawTest {

        @Test
        @DisplayName("회원 탈퇴 시 withdrawn = Y 가 되고, 개인식별정보(PII)가 파기된다")
        void withdrawUser_ShouldMarkWithdrawnAndNullifyPii() {
            // given - 개인정보가 존재하는 정상 계정

            // when
            user.withdrawUser();

            // then
            assertThat(user.getWithdrawn()).isEqualTo("Y");
            assertThat(user.getEmail()).isNull();
            assertThat(user.getPhoneNumber()).isNull();
            assertThat(user.getBirthDate()).isNull();
            assertThat(user.getUserName()).isEqualTo("WITHDRAWN_USER");
            assertThat(user.getPassword()).isBlank(); // 빈 문자열로 덮어씀
        }
    }

    // ===== 휴면 전환 =====

    @Nested
    @DisplayName("휴면 전환")
    class DormantTest {

        @Test
        @DisplayName("휴면 전환 시 accountLocked = Y 가 되고, 개인식별정보가 제거된다")
        void convertToDormant_ShouldLockAccountAndClearPii() {
            // given - 활성 상태의 정상 계정

            // when
            user.convertToDormant();

            // then
            assertThat(user.getAccountLocked()).isEqualTo("Y");
            assertThat(user.getUserName()).isEqualTo("DORMANT_USER");
            assertThat(user.getEmail()).isNull();
            assertThat(user.getPhoneNumber()).isNull();
        }
    }

    // ===== 비밀번호 변경 =====

    @Nested
    @DisplayName("비밀번호 변경")
    class PasswordTest {

        @Test
        @DisplayName("비밀번호 변경 시 새 해시값으로 교체되고 변경 시각이 기록된다")
        void changePassword_ShouldUpdatePasswordHashAndTimestamp() {
            // given
            String newPasswordHash = "newBcryptHash123!";

            // when
            user.changePassword(newPasswordHash);

            // then
            assertThat(user.getPassword()).isEqualTo(newPasswordHash);
            assertThat(user.getPasswordUpdated()).isNotNull();
        }
    }
}
