package com.koo.bonscore.sample.service;

import com.koo.bonscore.sample.mapper.SampleMapper;
import com.koo.bonscore.sample.vo.SampleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SampleService {

    @Autowired
    public SampleMapper sampleMapper;

    public List<SampleVo> test(SampleVo vo) {
        System.out.println(":: Test Start ::");
        List<SampleVo> result = sampleMapper.testSelect();
        System.out.println("cnt = " + result);
        System.out.println(":: Test End ::");

        return result;
    }
}
