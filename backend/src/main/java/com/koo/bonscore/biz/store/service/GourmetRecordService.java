package com.koo.bonscore.biz.store.service;

import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.GourmetRecordDto;
import com.koo.bonscore.biz.store.mapper.GourmetRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GourmetRecordService {

    private final GourmetRecordMapper gourmetRecordMapper;

    @Transactional
    public void createGourmetRecord(GourmetRecordCreateRequest request) {
        // 1. 맛집 기록(부모) 정보 저장
        //    Mapper에서 useGeneratedKeys 또는 selectKey를 사용하여 request 객체의 recordId 필드에 자동 생성된 ID가 채워짐

        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        if(request.getRecordId() == null) {
            request.setRecordId(gourmetRecordMapper.selectRecordId());
        }

        gourmetRecordMapper.insertGourmetRecord(request);

        // 2. 이미지 정보(자식)가 있는 경우에만 저장
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // 부모 테이블에서 생성된 recordId를 자식 DTO 리스트에 설정
            request.getImages().forEach(image -> image.setRecordId(request.getRecordId()));

            // 이미지 리스트를 한번의 쿼리로 저장 (batch insert)
            gourmetRecordMapper.insertGourmetImages(request);
        }
    }

    @Transactional(readOnly = true)
    public List<GourmetRecordDto> getGourmetRecords(GourmetRecordCreateRequest request) {
        return gourmetRecordMapper.selectGourmetRecordsByUserId(request.getUserId());
    }
}
