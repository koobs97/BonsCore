package com.koo.bonscore.biz.authorization.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * Role.java
 * 설명 : 사용자 권한(Role) 정보를 관리하는 엔티티
 *       (예: ADMIN, USER 등)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
@Entity
@Table(name = "ROLE")
@Getter
public class Role {

    @Id
    @Column(name = "ROLE_ID")
    private String roleId;

    @Column(name = "ROLE_NAME")
    private String roleName;

    /**
     * 해당 권한이 접근 가능한 메뉴 목록 (다대다 매핑)
     * <p>
     * - DB에는 'ROLE_MENU'라는 연결(매핑) 테이블이 생성됨.
     * - FetchType.LAZY: 권한 정보를 가져올 때 메뉴까지 당장 다 가져오진 않음 (필요할 때 조회).
     * - @OrderBy: 메뉴를 가져올 때 sortOrder 기준으로 정렬.
     * </p>
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "ROLE_MENU",
            joinColumns = @JoinColumn(name = "ROLE_ID"),
            inverseJoinColumns = @JoinColumn(name = "MENU_ID"))
    @OrderBy("sortOrder ASC") // 메뉴 가져올 때 자동 정렬
    private List<Menu> menus = new ArrayList<>();
}
