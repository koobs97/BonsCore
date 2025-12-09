package com.koo.bonscore.common.message.repository;

import com.koo.bonscore.common.message.entity.MessageResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageResourceRepository extends JpaRepository<MessageResource, Long> {

    /**
     * 어플리케이션 전역으로 쓰일 메시지 조회
     * @param locale 언어('ko', 'en')
     * @return 메시지 객체
     */
    List<MessageResource> findAllByLocale(String locale);

    /**
     * 관리자 화면 검색용 쿼리
     * 1. locale이 'ALL'이면 전체 언어, 아니면 해당 언어 일치
     * 2. keyword가 없거나('') null이면 전체 검색
     * 3. keyword가 있으면 code 또는 message에 포함(LIKE)되는 것 검색
     */
    @Query("SELECT m FROM MessageResource m " +
            "WHERE (:locale = 'ALL' OR m.locale = :locale) " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "     OR m.code LIKE %:keyword% " +
            "     OR m.message LIKE %:keyword%) " +
            "ORDER BY MIN(m.id) OVER (PARTITION BY m.code), m.code, m.id")
    List<MessageResource> searchByLocaleAndKeyword(@Param("locale") String locale,
                                                   @Param("keyword") String keyword);
}
