package com.koo.bonscore.biz.store.service;

import com.koo.bonscore.biz.store.dto.req.GourmetImageDto;
import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.GourmetRecordDto;
import com.koo.bonscore.biz.store.entity.GourmetImage;
import com.koo.bonscore.biz.store.entity.GourmetRecord;
import com.koo.bonscore.biz.store.repository.GourmetRecordRepository;
import com.koo.bonscore.common.api.google.service.GoogleTranslateService;
import com.koo.bonscore.common.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    private final GourmetRecordRepository gourmetRecordRepository; // Mapper -> Repository
    private final FileStorageService fileStorageService;
    private final GoogleTranslateService googleTranslateService;

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    /**
     * 저장소 레코드 저장 (JPA 방식)
     * insert/update/delete를 save() 하나와 Entity 상태 변경으로 처리
     */
    @Transactional
    public void saveGourmetRecord(GourmetRecordCreateRequest request) {

        // 1. Entity 조회 또는 생성
        GourmetRecord record;
        if (request.getRecordId() != null) {
            record = gourmetRecordRepository.findById(request.getRecordId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 기록을 찾을 수 없습니다."));
            // Update fields
            record.update(
                    request.getName(),
                    request.getCategory(),
                    parseDate(request.getVisitDate()),
                    request.getRating(),
                    request.getMemo(),
                    request.getReferenceUrl()
            );
        } else {
            // New Record
            record = GourmetRecord.builder()
                    .userId(request.getUserId())
                    .storeName(request.getName())
                    .category(request.getCategory())
                    .visitDate(parseDate(request.getVisitDate()))
                    .rating(request.getRating())
                    .memo(request.getMemo())
                    .referenceUrl(request.getReferenceUrl())
                    .build();
        }

        // 2. 임시 저장 및 ID 생성을 위해 먼저 save (Parent)
        // JPA는 영속성 컨텍스트가 관리하므로, ID가 필요하면 먼저 save 호출해도 됨
        // 만약 이미 ID가 있었다면 이 시점엔 아무 일도 안 일어날 수 있음 (트랜잭션 종료 시 flush)
        GourmetRecord savedRecord = gourmetRecordRepository.save(record);

        // 3. 이미지 리스트 동기화 (기존 리스트 clear -> 새 리스트 add)
        // orphanRemoval = true 덕분에 리스트에서 제거된 이미지는 DB에서도 삭제됨
        updateImages(savedRecord, request.getImages());
    }

    private void updateImages(GourmetRecord record, List<GourmetImageDto> imageDtos) {
        if (imageDtos == null) imageDtos = new ArrayList<>();

        // 영구 저장 경로 설정
        String permanentSubPath = "gourmet-records/" + record.getRecordId();

        // 기존 이미지 리스트를 비우고 새로 채워넣는 방식 (간편함)
        // *주의: 실제 프로덕션에서는 기존 이미지를 유지하고 싶은 경우, ID 비교 로직을 추가하여 UPDATE/INSERT를 구분해야 함.
        // 여기서는 요청받은 리스트가 '최종 상태'라고 가정하고 싹 갈아끼우는 로직으로 작성 (MyBatis의 deleteImagesNotInList와 유사 효과)
        record.getImages().clear();

        for (int i = 0; i < imageDtos.size(); i++) {
            GourmetImageDto dto = imageDtos.get(i);

            // 파일 이동 로직
            String finalStoredName = dto.getStoredFileName();
            String finalImageUrl = dto.getImageUrl();

            try {
                if (isTempFile(dto.getStoredFileName())) {
                    String permanentFilePath = fileStorageService.moveToPermanentStorage(dto.getStoredFileName(), permanentSubPath);
                    finalStoredName = permanentFilePath;
                    finalImageUrl = fileStorageService.getFileDownloadUri(permanentFilePath);
                }
            } catch (IOException e) {
                throw new RuntimeException("파일 이동 실패", e);
            }

            // Entity 생성 및 추가
            GourmetImage imageEntity = GourmetImage.builder()
                    .originalFileName(dto.getOriginalFileName())
                    .storedFileName(finalStoredName)
                    .imageUrl(finalImageUrl)
                    .fileSize(dto.getFileSize())
                    .imageOrder(i) // 순서 재설정
                    .build();

            record.addImage(imageEntity); // 편의 메서드 사용
        }
    }

    /**
     * 조회
     */
    @Transactional(readOnly = true)
    public List<GourmetRecordDto> getGourmetRecords(String userId, String lang) {
        // 1. Repository 조회 (Fetch Join으로 이미지까지 한번에)
        List<GourmetRecord> entities = gourmetRecordRepository.findAllByUserIdWithImages(userId);

        // 2. Entity -> DTO 변환
        List<GourmetRecordDto> dtos = entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // 3. 번역 로직 (기존 로직 유지)
        if (!"ko".equalsIgnoreCase(lang) && !dtos.isEmpty()) {
            translateDtos(dtos, lang);
        }

        // 4. 이미지 URL 가공 (기존 로직 유지)
        dtos.forEach(record -> {
            if (record.getImages() != null) {
                record.getImages().forEach(image -> {
                    String fullUrl = fileStorageService.getFileDownloadUri(image.getStoredFileName());
                    image.setImageUrl(fullUrl);
                });
            }
        });

        return dtos;
    }

    // Entity -> DTO 매퍼
    private GourmetRecordDto convertToDto(GourmetRecord entity) {
        GourmetRecordDto dto = new GourmetRecordDto();
        dto.setId(entity.getRecordId());
        dto.setName(entity.getStoreName());
        dto.setCategory(entity.getCategory());
        dto.setVisitDate(entity.getVisitDate());
        dto.setRating(entity.getRating());
        dto.setMemo(entity.getMemo());
        dto.setReferenceUrl(entity.getReferenceUrl());

        if (entity.getImages() != null) {
            List<GourmetImageDto> imageDtos = entity.getImages().stream().map(img -> {
                GourmetImageDto imgDto = new GourmetImageDto();
                imgDto.setImageId(img.getImageId());
                imgDto.setOriginalFileName(img.getOriginalFileName());
                imgDto.setStoredFileName(img.getStoredFileName());
                imgDto.setImageUrl(img.getImageUrl());
                imgDto.setFileSize(img.getFileSize());
                imgDto.setImageOrder(img.getImageOrder());
                return imgDto;
            }).collect(Collectors.toList());
            dto.setImages(imageDtos);
        }
        return dto;
    }

    // 번역 처리 분리
    private void translateDtos(List<GourmetRecordDto> records, String lang) {
        List<String> textsToTranslate = new ArrayList<>();
        for (GourmetRecordDto record : records) {
            textsToTranslate.add(cleanAndUnescape(record.getName()));
            textsToTranslate.add(cleanAndUnescape(record.getCategory()));
            textsToTranslate.add(StringUtils.hasText(record.getMemo()) ? cleanAndUnescape(record.getMemo()) : "");
        }

        List<String> translatedTexts = googleTranslateService.translateTexts(textsToTranslate, "ko", lang);

        if (translatedTexts.size() == textsToTranslate.size()) {
            int idx = 0;
            for (GourmetRecordDto record : records) {
                record.setName(HtmlUtils.htmlUnescape(translatedTexts.get(idx++)));
                record.setCategory(HtmlUtils.htmlUnescape(translatedTexts.get(idx++)));
                if (StringUtils.hasText(record.getMemo())) {
                    record.setMemo(HtmlUtils.htmlUnescape(translatedTexts.get(idx++)));
                } else {
                    idx++;
                }
            }
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) return null;
        try {
            return LocalDate.parse(dateStr); // "yyyy-MM-dd" 형식 가정
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isTempFile(String storedFileName) {
        if (storedFileName == null) return false;
        return !storedFileName.contains("/") && !storedFileName.contains("\\");
    }

    private String cleanAndUnescape(String text) {
        if (!StringUtils.hasText(text)) return text;
        String noHtml = HTML_TAG_PATTERN.matcher(text).replaceAll("");
        return HtmlUtils.htmlUnescape(noHtml);
    }
}
