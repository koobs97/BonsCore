package com.koo.bonscore.sample;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SampleMapper {
    @Select("SELECT COUNT(1) FROM TB_USER")
    int testSelect();

}
