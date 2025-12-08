package com.koo.bonscore.log.repository;

import com.koo.bonscore.log.entity.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * <pre>
 * UserActivityLogRepository.java
 * 설명 : 사용자 활동 로그(UserActivityLog)에 접근하기 위한 저장소
 *       (로그 검색 및 필터링 옵션 조회 기능 포함)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long>, JpaSpecificationExecutor<UserActivityLog> {

    /**
     * DB에 저장된 활동 유형(Activity Type) 목록 조회
     * (검색 화면의 '활동 유형' 드롭다운 박스 구성용)
     *
     * @return 활동 유형 리스트 (예: ["LOGIN", "LOGOUT", "MENU"])
     */
    @Query("SELECT DISTINCT l.activityType FROM UserActivityLog l")
    List<String> findDistinctActivityTypes();

    /**
     * DB에 저장된 활동 결과(Activity Result) 목록 조회
     * (검색 화면의 '결과' 드롭다운 박스 구성용)
     *
     * @return 활동 결과 리스트 (예: ["SUCCESS", "FAIL"])
     */
    @Query("SELECT DISTINCT l.activityResult FROM UserActivityLog l")
    List<String> findDistinctActivityResults();
}
