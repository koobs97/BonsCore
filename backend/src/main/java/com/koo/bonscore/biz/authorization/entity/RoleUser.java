package com.koo.bonscore.biz.authorization.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

/**
 * <pre>
 * RoleUser.java
 * 설명 : 사용자(User)와 권한(Role)을 연결하는 매핑 엔티티
 *       (다대다 관계를 1:N, N:1로 풀어낸 중간 테이블)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
@Entity
@Table(name = "ROLE_USER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@IdClass(RoleUserId.class) // 복합키 클래스 지정
public class RoleUser implements Persistable<RoleUserId> {

    @Id
    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Id
    @Column(name = "ROLE_ID", nullable = false, length = 50)
    private String roleId;

    /**
     * Persistable 구현:
     * JPA는 @Id가 설정되어 있으면 save() 시 이미 존재하는지 확인하기 위해 SELECT를 먼저 날립니다.
     * 이를 방지하고 바로 INSERT를 날리기 위해 isNew를 항상 true로 반환하게 하거나,
     * 별도의 로직을 구성합니다. (여기서는 단순하게 새로운 객체 생성 시 true로 간주하도록 구현)
     */
    @Transient
    private boolean isNew = true;

    @Override
    public RoleUserId getId() {
        return new RoleUserId(userId, roleId);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    // 영속화 후에는 false로 변경 (JPA 콜백)
    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
