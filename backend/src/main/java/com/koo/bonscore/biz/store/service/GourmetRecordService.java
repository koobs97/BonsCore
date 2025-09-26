package com.koo.bonscore.biz.store.service;

import com.koo.bonscore.biz.store.dto.req.GourmetImageDto;
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
import java.util.stream.IntStream;

/**
 * <pre>
 * GourmetRecordService.java
 * 설명 : 저장소 관리 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-09-24
 */
@Service
@RequiredArgsConstructor
public class GourmetRecordService {

    private final GourmetRecordMapper gourmetRecordMapper;

    /**
     * 저장소 레코드 저장
     * @param request 작성한 데이터 및 이미지
     */
    @Transactional
    public void saveGourmetRecord(GourmetRecordCreateRequest request) {

        request.setUpdatedAt(LocalDateTime.now());
        if (request.getRecordId() == null) {
            // 새 레코드인 경우
            request.setRecordId(gourmetRecordMapper.selectRecordId());
            request.setCreatedAt(LocalDateTime.now());
        }
        gourmetRecordMapper.mergeGourmetRecord(request);


        // 프론트에서 전달된 이미지 목록에 없는 파일들을 먼저 삭제
        Map<String, Object> params = new HashMap<>();
        params.put("recordId", request.getRecordId());
        params.put("images", request.getImages());
        gourmetRecordMapper.deleteImagesNotInList(params);


        if (request.getImages() != null && !request.getImages().isEmpty()) {

            // 이미지가 들어온 순서대로 저장 및 순서지정
            IntStream.range(0, request.getImages().size()).forEach(index -> {
                GourmetImageDto image = request.getImages().get(index);
                image.setRecordId(request.getRecordId());
                image.setImageOrder(index);
                // 프론트 배열의 순서(index)를 DTO에 저장
            });

            gourmetRecordMapper.mergeGourmetImages(request);
        }
    }

    /**
     * 조회
     * @param request GourmetRecordCreateRequest
     * @return List<GourmetRecordDto>
     */
    @Transactional(readOnly = true)
    public List<GourmetRecordDto> getGourmetRecords(GourmetRecordCreateRequest request) {
        return gourmetRecordMapper.selectGourmetRecordsByUserId(request.getUserId());
    }
}
