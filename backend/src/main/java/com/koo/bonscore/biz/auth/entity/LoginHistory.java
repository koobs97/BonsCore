package com.koo.bonscore.biz.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * <pre>
 * LoginHistory.java
 * 설명 : 사용자 로그인 이력을 저장하는 엔티티
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
@Entity
@Table(name = "LOGIN_HISTORY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "login_history_seq")
    @SequenceGenerator(
            name = "login_history_seq",
            sequenceName = "SEQ_LOGIN_HISTORY",
            allocationSize = 1
    )
    @Column(name = "HISTORY_SEQ")
    private Long id;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "IP_ADDRESS")
    private String ipAddress;

    @Column(name = "COUNTRY_CODE")
    private String countryCode;

    @Column(name = "LOGIN_AT")
    private LocalDateTime loginAt;
}
