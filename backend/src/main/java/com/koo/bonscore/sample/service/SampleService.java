package com.koo.bonscore.sample.service;

import com.koo.bonscore.sample.mapper.SampleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SampleService {

    @Autowired
    public SampleMapper sampleMapper;

    public void test() {
        System.out.println(":: Test Start ::");
        int cnt = sampleMapper.testSelect();
        System.out.println(":: Test End ::");
    }
}
