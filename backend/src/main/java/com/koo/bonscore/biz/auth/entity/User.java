package com.koo.bonscore.biz.auth.entity;

import com.koo.bonscore.biz.authorization.entity.Role;
import com.koo.bonscore.biz.authorization.entity.RoleUser;
import com.koo.bonscore.biz.authorization.entity.SecurityQuestion;
import com.koo.bonscore.biz.users.entity.UserDormantInfo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * User.java
 * 설명 : User entity
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
@Entity
@Table(name = "USER_INFO")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@DynamicUpdate
public class User {

    @Id
    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "USER_NAME", nullable = false, length = 256)
    private String userName;

    @Column(name = "EMAIL", unique = true, length = 256)
    private String email;

    @Column(name = "EMAIL_HASH", unique = true, length = 256)
    private String emailHash;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String password;

    @Column(name = "PHONE_NUMBER", length = 256)
    private String phoneNumber;

    @Column(name = "BIRTH_DATE", length = 256)
    private String birthDate;

    @Column(name = "GENDER_CODE", length = 1)
    private String genderCode;

    @Column(name = "OAUTH_PROVIDER", length = 50)
    private String oauthProvider;

    @Column(name = "OAUTH_PROVIDER_ID")
    private String oauthProviderId;

    @Column(name = "PASSWORD_HINT", length = 50)
    private String passwordHint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PASSWORD_HINT", insertable = false, updatable = false)
    private SecurityQuestion securityQuestion;

    @Column(name = "PASSWORD_HINT_ANSWER")
    private String passwordHintAnswer;

    @Column(name = "PASSWORD_UPDATED")
    private LocalDateTime passwordUpdated;

    @Builder.Default
    @Column(name = "ACCOUNT_LOCKED", length = 1)
    private String accountLocked = "N";

    @Builder.Default
    @Column(name = "REQUIRES_VERIFICATION_YN", length = 1)
    private String requiresVerificationYn = "N";

    @Builder.Default
    @Column(name = "WITHDRAWN", length = 1)
    private String withdrawn = "N";

    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;

    @Builder.Default
    @Column(name = "TERMS_AGREE_1", length = 1)
    private String termsAgree1 = "N";

    @Builder.Default
    @Column(name = "TERMS_AGREE_2", length = 1)
    private String termsAgree2 = "N";

    @Builder.Default
    @Column(name = "TERMS_AGREE_3", length = 1)
    private String termsAgree3 = "N";

    @Builder.Default
    @Column(name = "TERMS_AGREE_4", length = 1)
    private String termsAgree4 = "N";

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "ROLE_USER",
            joinColumns = @JoinColumn(name = "USER_ID"),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID"))
    private List<Role> roles = new ArrayList<>();

    /**
     * 자동 시간 업데이트
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.passwordUpdated == null) {
            this.passwordUpdated = now;
        }
    }

    /**
     * 자동 시간 업데이트
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 로그인 성공 시 시간 업데이트
     */
    public void updateLoginTime() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 비정상 로그인 시도 등으로 인한 계정 잠금/추가인증 요구
     */
    public void lockForVerification() {
        this.requiresVerificationYn = "Y";
    }

    /**
     * 계정 잠금 해제 및 인증 완료 처리
     */
    public void unlockAccount() {
        this.requiresVerificationYn = "N";
        this.accountLocked = "N";
    }

    /**
     * 소셜 계정 연동 정보 업데이트
     * @param provider 소셜 서비스 구분
     * @param providerId 소셜 서비스 발급 ID
     */
    public void linkSocialAccount(String provider, String providerId) {
        this.oauthProvider = provider;
        this.oauthProviderId = providerId;
    }

    /**
     * 비밀번호 변경
     * @param newPasswordHash 신규 비밀번호
     */
    public void changePassword(String newPasswordHash) {
        this.password = newPasswordHash;
        this.passwordUpdated = LocalDateTime.now();
    }

    /**
     * 회원 탈퇴 처리
     */
    public void withdrawUser() {
        this.withdrawn = "Y";
        this.updatedAt = LocalDateTime.now();

        // PII(개인식별정보) 파기
        this.userName = "WITHDRAWN_USER"; // NOT NULL 컬럼인 경우 의미 없는 값으로 대체
        this.email = null;
        this.emailHash = null;
        this.phoneNumber = null;
        this.birthDate = null;
        this.genderCode = null;
        this.password = ""; // 비밀번호 삭제

        // 소셜 연동 정보 삭제
        this.oauthProvider = null;
        this.oauthProviderId = null;

        // 보안 질문 등 기타 정보 초기화
        this.passwordHint = null;
        this.passwordHintAnswer = null;

        // 약관 동의 내역 초기화
        this.termsAgree1 = "N";
        this.termsAgree2 = "N";
        this.termsAgree3 = "N";
        this.termsAgree4 = "N";
    }

    /**
     * 휴면 상태로 전환 (개인정보 파기 및 잠금)
     */
    public void convertToDormant() {
        this.userName = "DORMANT_USER"; // 혹은 null/마스킹 처리
        this.email = null;
        this.emailHash = null;
        this.phoneNumber = null;
        this.birthDate = null;
        this.genderCode = null;
        this.accountLocked = "Y";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 휴면 해제 (개인정보 및 설정 복원)
     * UserDormantInfo에 저장된 모든 정보를 받아서 복구합니다.
     */
    public void restoreFromDormancy(UserDormantInfo dormantInfo) {
        this.userName = dormantInfo.getUserName();
        this.email = dormantInfo.getEmail();
        this.emailHash = dormantInfo.getEmailHash();
        this.phoneNumber = dormantInfo.getPhoneNumber();
        this.birthDate = dormantInfo.getBirthDate();
        this.genderCode = dormantInfo.getGenderCode();

        // 추가된 정보 복원
        this.passwordHint = dormantInfo.getPasswordHint();
        this.passwordHintAnswer = dormantInfo.getPasswordHintAnswer();
        this.passwordUpdated = dormantInfo.getPasswordUpdated();
        this.termsAgree1 = dormantInfo.getTermsAgree1();
        this.termsAgree2 = dormantInfo.getTermsAgree2();
        this.termsAgree3 = dormantInfo.getTermsAgree3();
        this.termsAgree4 = dormantInfo.getTermsAgree4();

        // 상태 변경
        this.accountLocked = "N";
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 회원 정보 수정
     */
    public void updateInfo(String userName, String email, String emailHash, String phoneNumber, String birthDate, String genderCode) {
        this.userName = userName;
        this.email = email;
        this.emailHash = emailHash;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.genderCode = genderCode;
    }

    /**
     * 비밀번호 업데이트 (기존 changePassword와 유사하나 용도 분리 가능)
     */
    public void updatePassword(String encryptedPassword) {
        this.password = encryptedPassword;
        this.passwordUpdated = LocalDateTime.now();
    }

    /**
     * 보안 질문/답변 변경
     */
    public void updateSecurityHint(String passwordHint, String passwordHintAnswer) {
        this.passwordHint = passwordHint;
        this.passwordHintAnswer = passwordHintAnswer;
    }

}
