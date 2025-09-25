package com.koo.bonscore.biz.store.mapper;

import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.GourmetRecordDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface GourmetRecordMapper {
    long selectRecordId();
    void deleteImagesNotInList(Map<String, Object> params);
    void mergeGourmetRecord(GourmetRecordCreateRequest dto);
    void mergeGourmetImages(GourmetRecordCreateRequest dto);
    List<GourmetRecordDto> selectGourmetRecordsByUserId(String userId);
}
