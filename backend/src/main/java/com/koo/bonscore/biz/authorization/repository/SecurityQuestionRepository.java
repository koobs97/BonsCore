package com.koo.bonscore.biz.authorization.repository;

import com.koo.bonscore.biz.authorization.entity.SecurityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * <pre>
 * SecurityQuestionRepository.java
 * 설명 : 보안 질문(SecurityQuestion) 엔티티에 접근하기 위한 저장소
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
public interface SecurityQuestionRepository extends JpaRepository<SecurityQuestion, String> {

    /**
     * 사용 여부(useYn)에 따라 보안 질문 목록을 조회하고,
     * 지정된 순서(displayOrder)대로 정렬하여 반환.
     *
     * @param useYn 사용 여부 ('Y': 사용 중인 질문만, 'N': 미사용 질문만)
     * @return 정렬된 보안 질문 리스트
     */
    List<SecurityQuestion> findAllByUseYnOrderByDisplayOrder(String useYn);
}
