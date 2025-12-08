package com.koo.bonscore.biz.authorization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <pre>
 * SecurityQuestion.java
 * 설명 : 보안 질문(비밀번호 찾기 질문)을 관리하는 마스터 데이터 엔티티
 *       (예: "가장 기억에 남는 장소는?", "어릴 적 별명은?" 등)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
@Entity
@Table(name = "SECURITY_QUESTION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SecurityQuestion {
    @Id
    @Column(name = "QUESTION_CODE")
    private String questionCode;

    @Column(name = "QUESTION_TEXT")
    private String questionText;

    @Column(name = "DISPLAY_ORDER")
    private Integer displayOrder;

    @Column(name = "USE_YN")
    private String useYn;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
