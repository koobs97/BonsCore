package com.koo.bonscore.sample.mapper;

import com.koo.bonscore.sample.vo.SampleVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SampleMapper {
    List<SampleVo> testSelect();

}
