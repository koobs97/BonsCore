package com.koo.bonscore.sample.mapper;

import com.koo.bonscore.core.config.annotation.Pageable;
import com.koo.bonscore.sample.vo.SampleVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SampleMapper {

    @Pageable
    List<SampleVo> testSelect(SampleVo vo);
}
