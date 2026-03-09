package com.koo.bonscore.biz.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.GourmetRecordDto;
import com.koo.bonscore.biz.store.service.GourmetRecordService;
import com.koo.bonscore.common.file.service.FileStorageService;
import com.koo.bonscore.core.config.web.WebConfig;
import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.config.web.security.config.LoginSessionManager;
import com.koo.bonscore.log.service.UserActivityLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GourmetRecordController 테스트
 *
 * @AuthenticationPrincipal을 사용하는 메서드 → @ExtendWith(MockitoExtension.class) 직접 호출로 테스트
 * HTTP 레이어 테스트가 필요한 메서드 → @WebMvcTest 슬라이스 테스트 사용
 */
@DisplayName("GourmetRecordController")
class GourmetRecordControllerTest {

    // ===== @AuthenticationPrincipal 사용 메서드 직접 호출 테스트 =====

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("@AuthenticationPrincipal 사용 메서드 (직접 호출)")
    class DirectCallTest {

        @InjectMocks
        private GourmetRecordController gourmetRecordController;

        @Mock
        private GourmetRecordService gourmetRecordService;

        @Mock
        private FileStorageService fileStorageService;

        @Test
        @DisplayName("getGourmetRecords() - 인증된 사용자의 맛집 기록 리스트를 반환한다")
        void getGourmetRecords_WhenAuthenticated_ReturnsRecordList() {
            // given
            given(gourmetRecordService.getGourmetRecords("testUser", "ko")).willReturn(List.of());
            User mockPrincipal = new User("testUser", "", List.of());

            // when
            ResponseEntity<List<GourmetRecordDto>> response = gourmetRecordController.getGourmetRecords("ko", mockPrincipal);

            // then
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isEmpty();
        }

        @Test
        @DisplayName("deleteOldTempFiles() - 인증된 사용자의 임시 파일 정리가 호출된다")
        void deleteOldTempFiles_WhenAuthenticated_CallsCleanup() {
            // given
            User mockPrincipal = new User("testUser", "", List.of());

            // when
            gourmetRecordController.deleteOldTempFiles(mockPrincipal);

            // then
            then(fileStorageService).should(times(1)).cleanupUserTempFiles("testUser");
        }
    }

    // ===== HTTP 레이어 테스트 (@WebMvcTest 슬라이스 테스트) =====

    @Nested
    @WebMvcTest(
            value = GourmetRecordController.class,
            excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
    )
    @DisplayName("POST /api/gourmet-records - 맛집 기록 저장")
    class CreateGourmetRecordTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean private GourmetRecordService gourmetRecordService;
        @MockitoBean private FileStorageService fileStorageService;
        @MockitoBean private UserActivityLogService userActivityLogService;
        @MockitoBean private JwtTokenProvider jwtTokenProvider;
        @MockitoBean private LoginSessionManager loginSessionManager;

        @Test
        @DisplayName("유효한 요청 시 200을 반환하고 서비스가 1회 호출된다")
        void createGourmetRecord_WhenValidRequest_Returns200() throws Exception {
            // given - void 메서드는 별도 설정 불필요
            GourmetRecordCreateRequest request = new GourmetRecordCreateRequest();
            request.setUserId("testUser");
            request.setName("맛있는 식당");
            request.setCategory("한식");
            request.setImages(new ArrayList<>());

            // when & then
            mockMvc.perform(post("/api/gourmet-records")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());

            then(gourmetRecordService).should(times(1)).saveGourmetRecord(any(GourmetRecordCreateRequest.class));
        }
    }
}
