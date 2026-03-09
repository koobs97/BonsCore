package com.koo.bonscore.biz.auth.service;

import com.koo.bonscore.biz.auth.controller.RSAController;
import com.koo.bonscore.biz.auth.dto.req.ClientInfoDto;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.auth.dto.res.LoginResponseDto;
import com.koo.bonscore.biz.authorization.entity.RoleUser;
import com.koo.bonscore.biz.authorization.repository.UserRoleRepository;
import com.koo.bonscore.biz.users.entity.User;
import com.koo.bonscore.biz.users.repository.UserDormantRepository;
import com.koo.bonscore.biz.users.repository.UserRepository;
import com.koo.bonscore.common.api.mail.service.MailService;
import com.koo.bonscore.core.config.enc.EncryptionService;
import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.log.repository.LoginHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

/**
 * AuthService 단위 테스트
 *
 * @ExtendWith(MockitoExtension.class): JUnit 5에서 Mockito를 활성화
 * @InjectMocks: 테스트 대상 객체 생성 + @Mock으로 선언한 의존성 자동 주입
 *
 * 핵심 검증 대상
 *  - login()          : 자격증명 실패, 휴면 계정, 정상 로그인
 *  - isDuplicateId()  : 아이디 중복 여부
 *  - signup()         : 유출 비밀번호 차단, 정상 저장
 *  - resetPassword()  : 토큰 유효성, 비밀번호 변경
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserRepository userRepository;
    @Mock private LoginHistoryRepository loginHistoryRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserDormantRepository userDormantRepository;
    @Mock private RSAController rsaController;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private EncryptionService encryptionService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private PwnedPasswordService pwnedPasswordService;
    @Mock private MailService mailService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    // ===== 로그인 =====

    @Nested
    @DisplayName("로그인 - login()")
    class LoginTest {

        // 테스트용 클라이언트 정보 (국가 코드 KR)
        private final ClientInfoDto clientInfo = ClientInfoDto.builder()
                .userId("testUser")
                .ipAddress("127.0.0.1")
                .userAgent("TestAgent/1.0")
                .countryCode("KR")
                .cityName("Seoul")
                .build();

        @Test
        @DisplayName("존재하지 않는 사용자로 로그인 시 BsCoreException이 발생한다")
        void login_WhenUserNotFound_ThrowsBsCoreException() throws Exception {
            // given
            given(rsaController.decrypt(anyString())).willReturn("plainPassword");
            given(userRepository.findByUserId(anyString())).willReturn(Optional.empty());

            LoginDto request = LoginDto.builder().userId("ghost").password("encPw").build();

            // when & then
            assertThatThrownBy(() -> authService.login(request, clientInfo))
                    .isInstanceOf(BsCoreException.class);
        }

        @Test
        @DisplayName("비밀번호 불일치 시 BsCoreException이 발생한다")
        void login_WhenPasswordNotMatch_ThrowsBsCoreException() throws Exception {
            // given
            User user = User.builder()
                    .userId("testUser")
                    .password("storedHash")
                    .build();
            given(rsaController.decrypt(anyString())).willReturn("wrongPassword");
            given(userRepository.findByUserId(anyString())).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrongPassword", "storedHash")).willReturn(false);

            LoginDto request = LoginDto.builder().userId("testUser").password("encPw").build();

            // when & then
            assertThatThrownBy(() -> authService.login(request, clientInfo))
                    .isInstanceOf(BsCoreException.class);
        }

        @Test
        @DisplayName("휴면 계정으로 로그인 시 success=false, reason=DORMANT_ACCOUNT 응답을 반환한다")
        void login_WhenAccountDormant_ReturnsDormantResponse() throws Exception {
            // given - 자격증명은 맞지만 accountLocked=Y(휴면) 상태
            User user = User.builder()
                    .userId("dormantUser")
                    .password("hash")
                    .accountLocked("Y")
                    .build();
            given(rsaController.decrypt(anyString())).willReturn("password");
            given(userRepository.findByUserId(anyString())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

            LoginDto request = LoginDto.builder().userId("dormantUser").password("encPw").build();

            // when
            LoginResponseDto result = authService.login(request, clientInfo);

            // then
            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getReason()).isEqualTo("DORMANT_ACCOUNT");
        }

        @Test
        @DisplayName("정상 로그인 시 success=true이고 accessToken이 포함된 응답을 반환한다")
        void login_WhenValidCredentials_ReturnsSuccessWithToken() throws Exception {
            // given
            // roles를 빈 리스트로 초기화 → getRoles() 내부 stream() NPE 방지
            User user = User.builder()
                    .userId("testUser")
                    .password("hash")
                    .accountLocked("N")
                    .requiresVerificationYn("N")
                    .birthDate("enc_bd")
                    .genderCode("M")
                    .phoneNumber("enc_phone")
                    .roles(new ArrayList<>())
                    .build();
            given(rsaController.decrypt(anyString())).willReturn("password");
            given(userRepository.findByUserId(anyString())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

            // Redis 캐시 히트: 이전 접속 국가(KR)와 현재 접속 국가(KR)가 동일 → 정상 로그인으로 판정
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(anyString())).willReturn("KR");

            given(jwtTokenProvider.createToken(anyString(), anyList(), anyLong())).willReturn("mock.jwt.token");

            LoginDto request = LoginDto.builder().userId("testUser").password("encPw").build();

            // when
            LoginResponseDto result = authService.login(request, clientInfo);

            // then
            assertThat(result.getSuccess()).isTrue();
            assertThat(result.getAccessToken()).isEqualTo("mock.jwt.token");
        }
    }

    // ===== 아이디 중복 체크 =====

    @Nested
    @DisplayName("아이디 중복 체크 - isDuplicateId()")
    class IsDuplicateIdTest {

        @Test
        @DisplayName("이미 존재하는 아이디면 true를 반환한다")
        void isDuplicateId_WhenUserExists_ReturnsTrue() {
            // given
            given(userRepository.existsByUserId("existingUser")).willReturn(true);
            SignUpDto request = SignUpDto.builder().userId("existingUser").build();

            // when
            boolean result = authService.isDuplicateId(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 아이디면 false를 반환한다")
        void isDuplicateId_WhenUserNotExists_ReturnsFalse() {
            // given
            given(userRepository.existsByUserId("newUser")).willReturn(false);
            SignUpDto request = SignUpDto.builder().userId("newUser").build();

            // when
            boolean result = authService.isDuplicateId(request);

            // then
            assertThat(result).isFalse();
        }
    }

    // ===== 회원가입 =====

    @Nested
    @DisplayName("회원가입 - signup()")
    class SignupTest {

        @Test
        @DisplayName("유출된 비밀번호로 가입 시도 시 BsCoreException이 발생한다")
        void signup_WhenPasswordPwned_ThrowsBsCoreException() throws Exception {
            // given - HaveIBeenPwned API에서 유출된 비밀번호로 판정
            given(rsaController.decrypt(anyString())).willReturn("pwned123!");
            given(pwnedPasswordService.isPasswordPwned(anyString())).willReturn(Mono.just(true));

            SignUpDto request = SignUpDto.builder().userId("newUser").password("encPw").build();

            // when & then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(BsCoreException.class);
        }

        @Test
        @DisplayName("정상 회원가입 시 User와 RoleUser가 각 1회 저장된다")
        void signup_WhenValidRequest_SavesUserAndRole() throws Exception {
            // given
            given(rsaController.decrypt(anyString())).willReturn("safePassword1!");
            given(pwnedPasswordService.isPasswordPwned(anyString())).willReturn(Mono.just(false));
            given(encryptionService.encrypt(anyString())).willReturn("encrypted");
            given(encryptionService.hashWithSalt(anyString())).willReturn("hashedEmail");
            given(passwordEncoder.encode(anyString())).willReturn("bcryptHash");

            SignUpDto request = SignUpDto.builder()
                    .userId("newUser").password("encPw").userName("홍길동")
                    .email("test@test.com").phoneNumber("01012345678")
                    .birthDate("19900101").genderCode("M")
                    .build();

            // when
            authService.signup(request);

            // then - User 저장 1회, RoleUser(USER 권한) 저장 1회 검증
            then(userRepository).should(times(1)).save(any(User.class));
            then(userRoleRepository).should(times(1)).save(any(RoleUser.class));
        }
    }

    // ===== 비밀번호 재설정 =====

    @Nested
    @DisplayName("비밀번호 재설정 - resetPasswordWithToken()")
    class ResetPasswordTest {

        @Test
        @DisplayName("유효하지 않은 토큰이면 BsCoreException이 발생한다")
        void resetPasswordWithToken_WhenTokenInvalid_ThrowsBsCoreException() {
            // given
            given(jwtTokenProvider.validateToken(anyString())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.resetPasswordWithToken("invalid.token", "encPw"))
                    .isInstanceOf(BsCoreException.class);
        }

        @Test
        @DisplayName("유효한 토큰이면 새로운 해시값으로 비밀번호가 변경된다")
        void resetPasswordWithToken_WhenTokenValid_ChangesPassword() throws Exception {
            // given
            String token = "valid.jwt.token";
            User user = User.builder().userId("testUser").password("oldHash").build();

            given(jwtTokenProvider.validateToken(token)).willReturn(true);
            given(jwtTokenProvider.getUserId(token)).willReturn("testUser");
            given(userRepository.findByUserId("testUser")).willReturn(Optional.of(user));
            given(rsaController.decrypt(anyString())).willReturn("newPassword");
            given(passwordEncoder.encode("newPassword")).willReturn("newBcryptHash");

            // when
            authService.resetPasswordWithToken(token, "encNewPw");

            // then - Dirty Checking으로 비밀번호가 새 해시값으로 교체됐는지 검증
            assertThat(user.getPassword()).isEqualTo("newBcryptHash");
        }
    }
}
