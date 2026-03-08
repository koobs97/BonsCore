package com.koo.bonscore.biz.users.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * <pre>
 * UserDormantInfo.java
 * 설명 : 휴면 사용자 정보 Entity
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
@Entity
@Table(name = "USER_DORMANT_INFO")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@DynamicInsert
@DynamicUpdate
public class UserDormantInfo {

    @Id
    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "USER_NAME", nullable = false, length = 256)
    private String userName;

    @Column(name = "EMAIL", unique = true, length = 256)
    private String email;

    @Column(name = "EMAIL_HASH", unique = true, length = 256)
    private String emailHash;

    @Column(name = "PHONE_NUMBER", length = 256)
    private String phoneNumber;

    @Column(name = "BIRTH_DATE", length = 256)
    private String birthDate;

    @Column(name = "GENDER_CODE", length = 1)
    private String genderCode;

    @Column(name = "PASSWORD_HINT", length = 255)
    private String passwordHint;

    @Column(name = "PASSWORD_HINT_ANSWER", length = 255)
    private String passwordHintAnswer;

    @Column(name = "PASSWORD_UPDATED")
    private LocalDateTime passwordUpdated;

    @Column(name = "TERMS_AGREE_1", length = 1)
    private String termsAgree1;

    @Column(name = "TERMS_AGREE_2", length = 1)
    private String termsAgree2;

    @Column(name = "TERMS_AGREE_3", length = 1)
    private String termsAgree3;

    @Column(name = "TERMS_AGREE_4", length = 1)
    private String termsAgree4;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    /**
     * 엔티티 저장 전 실행 (Default Value 처리)
     */
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.passwordUpdated == null) {
            this.passwordUpdated = LocalDateTime.now();
        }
        // 기본값 설정 (null일 경우 'N' 처리)
        if (this.termsAgree1 == null) this.termsAgree1 = "N";
        if (this.termsAgree2 == null) this.termsAgree2 = "N";
        if (this.termsAgree3 == null) this.termsAgree3 = "N";
        if (this.termsAgree4 == null) this.termsAgree4 = "N";
    }
}
