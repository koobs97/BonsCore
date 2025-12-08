package com.koo.bonscore.log.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * <pre>
 * UserActivityLog.java
 * 설명 : 사용자 활동 로그를 기록하는 엔티티
 *       (API 요청 정보, 수행 결과, 에러 메시지 등을 저장하여 추적/감사 용도로 사용)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
@Entity
@Table(name = "USER_ACTIVITY_LOG")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 무분별한 객체 생성 방지
@AllArgsConstructor
@Builder
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Oracle IDENTITY 컬럼 매핑
    @Column(name = "LOG_ID")
    private Long logId;

    @Column(name = "USER_ID", length = 50)
    private String userId;

    @Column(name = "ACTIVITY_TYPE", nullable = false, length = 30)
    private String activityType;

    @Column(name = "ACTIVITY_RESULT", nullable = false, length = 10)
    private String activityResult;

    @Column(name = "REQUEST_IP", nullable = false, length = 50)
    private String requestIp;

    @Column(name = "REQUEST_URI", nullable = false) // 기본 길이 255
    private String requestUri;

    @Column(name = "REQUEST_METHOD", nullable = false, length = 10)
    private String requestMethod;

    /**
     * Oracle CLOB 타입 매핑
     * 대용량 텍스트 저장을 위해 @Lob 사용
     */
    @Lob
    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Column(name = "USER_AGENT", length = 500)
    private String userAgent;

    /**
     * 엔티티가 저장될 때 자동으로 현재 시간 설정
     * updatable = false : 로그 생성일은 수정되지 않음
     */
    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
