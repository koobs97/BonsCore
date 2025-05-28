package com.koo.bonscore.sample.service;

import com.koo.bonscore.common.paging.Page;
import com.koo.bonscore.common.paging.PageContext;
import com.koo.bonscore.common.paging.PageResult;
import com.koo.bonscore.sample.mapper.SampleMapper;
import com.koo.bonscore.sample.vo.SampleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SampleService {

    @Autowired
    public SampleMapper sampleMapper;

    @Transactional(readOnly = true)
    public PageResult<SampleVo> test(SampleVo vo) {


//        vo.setPage(new Page(1, 10));
        System.out.println(":: Test Start ::");
        System.out.println("vo : " + vo.toString());
        List<SampleVo> list = sampleMapper.testSelect(vo);
        int totalCount = PageContext.getTotalCount() != null ? PageContext.getTotalCount() : 0;

        System.err.println("totalCount :" + totalCount);
        System.err.println(":: Test End ::");

        // 사용 후 메모리 누수 방지를 위해 clear()
        PageContext.clear();


        return new PageResult<>(list, totalCount, 1, totalCount);
    }
}
