package com.koo.bonscore.sample.service;

import com.koo.bonscore.common.paging.dto.res.PageResult;
import com.koo.bonscore.common.paging.support.PageResultFactory;
import com.koo.bonscore.sample.mapper.SampleMapper;
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
     * @param vo
     * @return PageResult
     */
    @Transactional
    public PageResult<PagingVo> test(PagingVo vo) {

        log.info("test Service Start");
        List<PagingVo> list = sampleMapper.testSelect(vo);
        log.info("test Service End");

        return PageResultFactory.create(list, vo);
    }
}
