package com.koo.bonscore.sample.service;

import com.koo.bonscore.common.masking.context.MaskingContext;
import com.koo.bonscore.common.paging.dto.res.PageResult;
import com.koo.bonscore.common.paging.support.PageResultFactory;
import com.koo.bonscore.core.config.web.security.util.CommandInjectionPreventUtil;
import com.koo.bonscore.sample.mapper.SampleMapper;
import com.koo.bonscore.sample.vo.MaskingVo;
import com.koo.bonscore.sample.vo.PagingVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <pre>
 * SampleService.java
 * 설명 : 샘플 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SampleService {

    private final SampleMapper sampleMapper;

    /**
     * 페이징 처리 샘플 서비스
     *
     * @param vo PagingVo
     * @return PageResult
     */
    @Transactional
    public PageResult<PagingVo> pagingTest(PagingVo vo) {

        log.info("pagingTest Start");
        List<PagingVo> list = sampleMapper.testSelect(vo);
        log.info("pagingTest End");

        return PageResultFactory.create(list, vo);
    }

    /**
     * 마스킹 처리 샘플 서비스
     *
     * @param vo MaskingVo
     * @return List<MaskingVo>
     */
    @Transactional
    public List<MaskingVo> maskingTest(MaskingVo vo) {

        log.info("maskingTest Start");

        MaskingContext.setMaskingEnabled(vo.getMaskingEnabled());
        List<MaskingVo> list = sampleMapper.testSelect2(vo);

        log.info("maskingTest End");
        return list;
    }

    /**
     * el-table 속도비교 서비스
     *
     * @param vo PagingVo
     * @return List<PagingVo>
     */
    @Transactional
    public List<PagingVo> tableSpeedTest(PagingVo vo) {

        log.info("tableSpeedTest Start");
        List<PagingVo> list = sampleMapper.testSelect3(vo);
        log.info("tableSpeedTest End");
        return list;
    }
}
