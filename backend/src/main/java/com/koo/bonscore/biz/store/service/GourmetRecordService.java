package com.koo.bonscore.biz.store.service;

import com.koo.bonscore.biz.store.dto.req.GourmetImageDto;
import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.GourmetRecordDto;
import com.koo.bonscore.biz.store.mapper.GourmetRecordMapper;
import com.koo.bonscore.common.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
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
    private final FileStorageService fileStorageService;

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


        // --- 2. 파일 이동 및 DTO 정보 업데이트 ---
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            final Long recordId = request.getRecordId();

            // 영구 저장될 하위 경로 (예: gourmet-records/123)
            String permanentSubPath = "gourmet-records/" + recordId;

            IntStream.range(0, request.getImages().size()).forEach(index -> {
                GourmetImageDto image = request.getImages().get(index);

                // ★★★ 2-1. 임시 파일을 영구 저장소로 이동
                // image.getStoredFileName()에는 프론트에서 보낸 임시 파일명(UUID.jpg)이 담겨 있어야 함
                try {
                    // isNewImage() 와 같은 플래그가 있다면 신규 파일일때만 이동하도록 분기 처리 가능
                    // 여기서는 모든 파일이 임시 업로드 되었다고 가정
                    if (isTempFile(image.getStoredFileName())) { // 임시 파일인지 확인하는 로직 (선택사항)
                        String permanentFilePath = fileStorageService.moveToPermanentStorage(image.getStoredFileName(), permanentSubPath);

                        // ★★★ 2-2. DTO의 정보를 실제 저장된 경로로 업데이트
                        image.setStoredFileName(permanentFilePath); // 예: gourmet-records/123/UUID.jpg
                        image.setImageUrl(fileStorageService.getFileDownloadUri(permanentFilePath));
                    }
                } catch (IOException e) {
                    // 파일 이동 실패 시, 트랜잭션 롤백을 위해 RuntimeException 발생
                    throw new RuntimeException("파일을 영구 저장소로 이동하는 데 실패했습니다. 파일명: " + image.getStoredFileName(), e);
                }

                // ★★★ 2-3. 레코드 ID와 순서 설정
                image.setRecordId(recordId);
                image.setImageOrder(index);
            });
        }

        // 프론트에서 전달된 이미지 목록에 없는 파일들을 먼저 삭제
        Map<String, Object> params = new HashMap<>();
        params.put("recordId", request.getRecordId());
        params.put("images", request.getImages());
        gourmetRecordMapper.deleteImagesNotInList(params);

        // 업데이트된 이미지 정보로 DB에 merge
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            gourmetRecordMapper.mergeGourmetImages(request);
        }
    }

    /**
     * 파일명이 경로 구분자를 포함하지 않으면 임시 파일로 간주하는 간단한 예시 메소드
     */
    private boolean isTempFile(String storedFileName) {
        if (storedFileName == null) return false;
        // 경로 구분자('/' 또는 '\')가 포함되어 있으면 이미 영구 저장된 파일로 판단
        return !storedFileName.contains("/") && !storedFileName.contains("\\");
    }

    /**
     * 조회
     * @param request GourmetRecordCreateRequest
     * @return List<GourmetRecordDto>
     */
    @Transactional(readOnly = true)
    public List<GourmetRecordDto> getGourmetRecords(GourmetRecordCreateRequest request) {

        List<GourmetRecordDto> records = gourmetRecordMapper.selectGourmetRecordsByUserId(request.getUserId());

        // 2. 조회된 데이터의 이미지 URL을 완성된 형태로 가공합니다.
        records.forEach(record -> {
            if (record.getImages() != null) {
                record.getImages().forEach(image -> {
                    // storedFileName (상대 경로)을 기반으로 완전한 영구 URL을 생성
                    String fullUrl = fileStorageService.getFileDownloadUri(image.getStoredFileName());
                    image.setImageUrl(fullUrl);
                });
            }
        });

        // 3. 완전한 URL이 포함된 DTO 목록을 반환합니다.
        return records;
    }
}
