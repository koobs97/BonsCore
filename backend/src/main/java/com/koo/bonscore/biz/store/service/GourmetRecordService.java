package com.koo.bonscore.biz.store.service;

import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.GourmetRecordDto;
import com.koo.bonscore.biz.store.mapper.GourmetRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GourmetRecordService {

    private final GourmetRecordMapper gourmetRecordMapper;

    @Transactional
    public void saveGourmetRecord(GourmetRecordCreateRequest request) {

        // ★★★★★★★★★★★★★ 1. 부모 테이블 저장 로직 (기존과 거의 동일) ★★★★★★★★★★★★★
        request.setUpdatedAt(LocalDateTime.now());
        if (request.getRecordId() == null) {
            // 새 레코드인 경우
            request.setRecordId(gourmetRecordMapper.selectRecordId());
            request.setCreatedAt(LocalDateTime.now());
        }
        gourmetRecordMapper.mergeGourmetRecord(request); // MERGE 구문이므로 INSERT/UPDATE 모두 처리


        // ★★★★★★★★★★★★★ 2. 이미지 삭제 로직 추가 ★★★★★★★★★★★★★
        // 프론트에서 전달된 이미지 목록에 없는 파일들을 먼저 삭제
        Map<String, Object> params = new HashMap<>();
        params.put("recordId", request.getRecordId());
        params.put("images", request.getImages());
        gourmetRecordMapper.deleteImagesNotInList(params);


        // ★★★★★★★★★★★★★ 3. 이미지 병합(MERGE) 로직으로 변경 ★★★★★★★★★★★★★
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // 부모 ID와 생성 시간을 자식 DTO에 설정
            request.getImages().forEach(image -> {
                image.setRecordId(request.getRecordId());
            });

            // 기존 INSERT 대신 MERGE 메소드 호출
            gourmetRecordMapper.mergeGourmetImages(request);
        }
    }

    @Transactional(readOnly = true)
    public List<GourmetRecordDto> getGourmetRecords(GourmetRecordCreateRequest request) {
        return gourmetRecordMapper.selectGourmetRecordsByUserId(request.getUserId());
    }
}
