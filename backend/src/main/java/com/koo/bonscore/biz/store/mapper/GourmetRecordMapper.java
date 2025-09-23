package com.koo.bonscore.biz.store.mapper;

import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.StoreDetailResponseDto;
import com.koo.bonscore.biz.store.dto.res.StoreSimpleResponseDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

// com.yourcompany.bonscore.domain.store.StoreMapper.java
@Mapper
public interface GourmetRecordMapper {
    void insertGourmetRecord(GourmetRecordCreateRequest dto);
    void insertGourmetImages(GourmetRecordCreateRequest dto);
}
