package com.koo.bonscore.biz.authorization.repository;

import com.koo.bonscore.biz.authorization.entity.RoleUser;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <pre>
 * RoleUserRepository.java
 * 설명 : 사용자-권한 매핑(RoleUser) 엔티티에 접근하기 위한 저장소
 *       (복합키 RoleUserId를 사용함에 주의)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
public interface UserRoleRepository extends JpaRepository<RoleUser, Long> {

}
