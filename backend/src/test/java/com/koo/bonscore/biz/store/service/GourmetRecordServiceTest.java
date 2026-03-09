package com.koo.bonscore.biz.store.service;

import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.GourmetRecordDto;
import com.koo.bonscore.biz.store.entity.GourmetRecord;
import com.koo.bonscore.biz.store.repository.GourmetRecordRepository;
import com.koo.bonscore.common.api.google.service.GoogleTranslateService;
import com.koo.bonscore.common.file.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

/**
 * GourmetRecordService 단위 테스트
 *
 * 핵심 검증 대상
 *  - saveGourmetRecord() : 신규 기록 저장, 존재하지 않는 기록 수정 시 예외
 *  - getGourmetRecords() : 기록 조회, 한국어 요청 시 번역 건너뜀
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GourmetRecordService")
class GourmetRecordServiceTest {

    @InjectMocks
    private GourmetRecordService gourmetRecordService;

    @Mock private GourmetRecordRepository gourmetRecordRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private GoogleTranslateService googleTranslateService;

    // ===== 맛집 기록 저장 =====

    @Nested
    @DisplayName("맛집 기록 저장 - saveGourmetRecord()")
    class SaveGourmetRecordTest {

        @Test
        @DisplayName("신규 기록(recordId=null)이면 save()가 1회 호출된다")
        void saveGourmetRecord_WhenNewRecord_SavesOnce() {
            // given
            GourmetRecord savedRecord = GourmetRecord.builder()
                    .userId("testUser")
                    .storeName("Test Restaurant")
                    .category("Korean")
                    .build();
            given(gourmetRecordRepository.save(any(GourmetRecord.class))).willReturn(savedRecord);

            GourmetRecordCreateRequest request = new GourmetRecordCreateRequest();
            request.setUserId("testUser");
            request.setName("Test Restaurant");
            request.setCategory("Korean");
            request.setImages(new ArrayList<>()); // 이미지 없음 → fileStorageService 호출 안 함

            // when
            gourmetRecordService.saveGourmetRecord(request);

            // then
            then(gourmetRecordRepository).should(times(1)).save(any(GourmetRecord.class));
        }

        @Test
        @DisplayName("존재하지 않는 recordId로 수정 요청 시 IllegalArgumentException이 발생한다")
        void saveGourmetRecord_WhenRecordNotFound_ThrowsException() {
            // given
            given(gourmetRecordRepository.findById(anyLong())).willReturn(java.util.Optional.empty());

            GourmetRecordCreateRequest request = new GourmetRecordCreateRequest();
            request.setRecordId(999L); // 존재하지 않는 ID
            request.setName("Updated Name");

            // when & then
            assertThatThrownBy(() -> gourmetRecordService.saveGourmetRecord(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ===== 맛집 기록 조회 =====

    @Nested
    @DisplayName("맛집 기록 조회 - getGourmetRecords()")
    class GetGourmetRecordsTest {

        @Test
        @DisplayName("기록이 없으면 빈 리스트를 반환한다")
        void getGourmetRecords_WhenNoRecords_ReturnsEmptyList() {
            // given
            given(gourmetRecordRepository.findAllByUserIdWithImages(anyString()))
                    .willReturn(List.of());

            // when
            List<GourmetRecordDto> result = gourmetRecordService.getGourmetRecords("testUser", "ko");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("lang=ko이면 번역 서비스를 호출하지 않는다")
        void getGourmetRecords_WhenLangIsKo_SkipsTranslation() {
            // given
            given(gourmetRecordRepository.findAllByUserIdWithImages(anyString()))
                    .willReturn(List.of());

            // when
            gourmetRecordService.getGourmetRecords("testUser", "ko");

            // then - 한국어 요청은 번역 서비스를 호출하지 않아야 함
            then(googleTranslateService).should(never()).translateTexts(any(), anyString(), anyString());
        }
    }
}
