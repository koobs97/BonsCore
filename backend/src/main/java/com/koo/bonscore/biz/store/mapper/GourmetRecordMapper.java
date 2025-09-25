package com.koo.bonscore.biz.store.mapper;

import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.GourmetRecordDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GourmetRecordMapper {
    long selectRecordId();
    void insertGourmetRecord(GourmetRecordCreateRequest dto);
    void insertGourmetImages(GourmetRecordCreateRequest dto);
    List<GourmetRecordDto> selectGourmetRecordsByUserId(String userId);
}
