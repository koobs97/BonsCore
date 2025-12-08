package com.koo.bonscore.biz.auth.repository;

import com.koo.bonscore.biz.auth.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * <pre>
 * LoginHistoryRepository.java
 * 설명 : 사용자 로그인 이력 repository
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    /**
     * 최근 로그인한 국가 코드 조회
     * @param userId 사용자 ID
     * @return 로그인 히스토리
     */
    List<LoginHistory> findTop10ByUserIdOrderByLoginAtDesc(String userId);
}
