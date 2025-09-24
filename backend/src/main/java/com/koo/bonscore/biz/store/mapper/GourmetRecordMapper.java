package com.koo.bonscore.biz.store.mapper;

import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GourmetRecordMapper {
    void insertGourmetRecord(GourmetRecordCreateRequest dto);
    void insertGourmetImages(GourmetRecordCreateRequest dto);
}
