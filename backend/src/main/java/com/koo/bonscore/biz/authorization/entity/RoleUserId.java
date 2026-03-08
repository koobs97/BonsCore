package com.koo.bonscore.biz.authorization.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RoleUser의 복합키(Composite Key) 정의 클래스
 * 규칙:
 * 1. Serializable 구현 필수
 * 2. equals, hashCode 구현 필수 (Lombok @EqualsAndHashCode 로 해결)
 * 3. 기본 생성자 필수
 * 4. 필드명과 타입이 Entity의 @Id 필드와 정확히 일치해야 함
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RoleUserId implements Serializable {
    private String userId;
    private String roleId;
}
