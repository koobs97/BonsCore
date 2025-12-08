package com.koo.bonscore.biz.authorization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <pre>
 * Menu.java
 * 설명 : 시스템 메뉴 구조를 관리하는 엔티티
 *       (화면 좌측 GNB/LNB 등의 계층형 메뉴 데이터)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
@Entity
@Table(name = "MENU")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu {

    @Id
    @Column(name = "MENU_ID", length = 10)
    private String menuId;

    @Column(name = "MENU_NAME", nullable = false, length = 100)
    private String menuName;

    @Column(name = "MENU_URL")
    private String menuUrl;

    @Column(name = "PARENT_MENU_ID", length = 10)
    private String parentMenuId;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Column(name = "IS_VISIBLE", nullable = false, length = 1)
    private String isVisible;

}
