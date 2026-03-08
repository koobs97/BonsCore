package com.koo.bonscore.biz.authorization.repository;

import com.koo.bonscore.biz.authorization.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * <pre>
 * MenuRepository.java
 * 설명 : 메뉴 엔티티(Menu)에 접근하기 위한 저장소
 *       (주로 메뉴 관리 기능이나, 전체 메뉴 트리 구성 시 사용)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
public interface MenuRepository extends JpaRepository<Menu, String> {

    /**
     * 전체 메뉴를 가져오되, 순서(sortOrder)대로 정렬
     * @return 메뉴리스트
     */
    List<Menu> findAllByOrderBySortOrderAsc();

}
