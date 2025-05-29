package com.koo.bonscore.sample.controller;

import com.koo.bonscore.common.paging.dto.res.PageResult;
import com.koo.bonscore.core.config.api.ApiResponse;
import com.koo.bonscore.sample.service.SampleService;
import com.koo.bonscore.sample.vo.PagingVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <pre>
 * SampleController.java
 * 설명 : 샘플 컨트롤러
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-25
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sample")
public class SampleController {

    /**
     * 샘플매퍼
     */
    private final SampleService sampleService;

    /**
     * 페이징 처리 샘플
     * @param vo
     * @return PageResult<PagingVo>
     */
    @PostMapping("/paging")
    public ApiResponse<PageResult<PagingVo>> test(@RequestBody PagingVo vo) {
        PageResult<PagingVo> result = sampleService.test(vo);
        return ApiResponse.success("조회 성공", result);
    }
}
