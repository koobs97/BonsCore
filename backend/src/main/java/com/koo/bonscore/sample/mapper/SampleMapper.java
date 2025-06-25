package com.koo.bonscore.sample.mapper;

import com.koo.bonscore.common.paging.annotation.Pageable;
import com.koo.bonscore.sample.vo.MaskingVo;
import com.koo.bonscore.sample.vo.PagingVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <pre>
 * SampleMapper.java
 * 설명 : 샘플 서비스용 매퍼 인터페이스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-25
 */
@Mapper
public interface SampleMapper {

    /**
     * 페이징 조회 예시 -> @Pageable을 통한 페이징 사용 활성화
     * @param vo PagingVo
     * @return List<PagingVo>
     */
    @Pageable
    List<PagingVo> testSelect(PagingVo vo);

    /**
     * 마스킹 조회 예시
     * @param vo MaskingVo
     * @return List<MaskingVo>
     */
    List<MaskingVo> testSelect2(MaskingVo vo);

    /**
     * el-table 성능비교 예시
     * @param vo PagingVo
     * @return List<PagingVo>
     */
    List<PagingVo> testSelect3(PagingVo vo);
}
