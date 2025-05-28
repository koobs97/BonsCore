package com.koo.bonscore.sample.controller;

import com.koo.bonscore.common.page.PageResult;
import com.koo.bonscore.sample.service.SampleService;
import com.koo.bonscore.sample.vo.SampleVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sample")
public class SampleController {

    private final SampleService sampleService;

    @PostMapping("/paging")
    public PageResult<SampleVo> test(SampleVo vo) {
        return sampleService.test(vo);
    }
}
