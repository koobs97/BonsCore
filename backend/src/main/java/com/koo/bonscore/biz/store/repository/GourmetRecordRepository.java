package com.koo.bonscore.biz.store.repository;

import com.koo.bonscore.biz.store.entity.GourmetRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * <pre>
 * GourmetRecordRepository.java
 * 설명 : 맛집 기록(GourmetRecord) 엔티티에 접근하기 위한 저장소
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
public interface GourmetRecordRepository extends JpaRepository<GourmetRecord, Long> {

    /**
     * 특정 사용자의 맛집 기록을 조회하되, 첨부된 이미지들도 한 번에 가져온다. (Fetch Join)
     *
     * <p>
     * <strong>[성능 최적화]</strong><br>
     * - 일반 조회 시 N+1 문제가 발생할 수 있어 FETCH JOIN을 사용함.<br>
     * - DISTINCT: 1:N 조인 시 부모(Record) 데이터가 뻥튀기(중복)되는 것을 방지.<br>
     * - LEFT JOIN: 이미지가 없는 기록도 누락 없이 조회하기 위함.
     * </p>
     *
     * @param userId 조회할 사용자 ID
     * @return 이미지 정보가 포함된 맛집 기록 리스트 (최신 방문순 정렬)
     */
    @Query("SELECT DISTINCT r FROM GourmetRecord r " +
            "LEFT JOIN FETCH r.images i " +
            "WHERE r.userId = :userId " +
            "ORDER BY r.visitDate DESC, r.createdAt DESC, i.imageOrder ASC")
    List<GourmetRecord> findAllByUserIdWithImages(@Param("userId") String userId);
}