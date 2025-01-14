package com.koo.bonscore.sample.controller;

import com.koo.bonscore.sample.service.SampleService;
import com.koo.bonscore.sample.vo.SampleVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sample")
public class SampleController {

    private final SampleService sampleService;

    @PostMapping("test")
    public Boolean test(SampleVo vo) {
        sampleService.test(vo);
        return true;
    }
}
