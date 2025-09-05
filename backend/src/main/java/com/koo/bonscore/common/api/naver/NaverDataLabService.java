package com.koo.bonscore.common.api.naver;

import com.koo.bonscore.common.api.naver.dto.datalab.DataLabRequestDto;
import com.koo.bonscore.common.api.naver.dto.datalab.DataLabResponseDto;
import com.koo.bonscore.common.api.naver.dto.datalab.KeywordGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Slf4j
@Service
public class NaverDataLabService {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private static final String API_URL = "https://openapi.naver.com/v1/datalab/search";

    // application.yml에 등록한 값을 주입받음
    public NaverDataLabService(@Value("${api.naver.client.id}") String clientId,
                               @Value("${api.naver.client.secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.webClient = WebClient.builder()
                .baseUrl(API_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Naver-Client-Id", clientId)
                .defaultHeader("X-Naver-Client-Secret", clientSecret)
                .build();
    }

    /**
     * 특정 키워드의 최근 3개월간 검색량 트렌드를 조회하는 메서드
     * @param query 분석할 키워드 (가게 이름 등)
     * @return DataLabResponseDto API 응답 결과
     */
    public DataLabResponseDto getSearchTrend(String query) {
        log.info("네이버 데이터랩 검색 트렌드 조회 시작: query=[{}]", query);

        // 최근 3개월 날짜 설정
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(3);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // API 요청 본문(Body) 생성
        KeywordGroup keywordGroup = KeywordGroup.builder()
                .groupName(query)
                .keywords(Collections.singletonList(query))
                .build();

        DataLabRequestDto requestDto = DataLabRequestDto.builder()
                .startDate(startDate.format(formatter))
                .endDate(endDate.format(formatter))
                .timeUnit("month") // "date", "week", "month" 중 선택
                .keywordGroups(Collections.singletonList(keywordGroup))
                .build();

        // WebClient를 이용한 비동기 API 호출
        try {
            DataLabResponseDto response = webClient.post()
                    .bodyValue(requestDto) // 요청 DTO를 body에 담음
                    .retrieve() // 응답을 받기 시작
                    .bodyToMono(DataLabResponseDto.class) // 응답 body를 DTO로 변환
                    .block(); // 동기식으로 결과를 기다림

            log.info("네이버 데이터랩 검색 트렌드 조회 성공");
            return response;
        } catch (Exception e) {
            log.error("네이버 데이터랩 API 호출 중 오류 발생", e);
            // 실제 서비스에서는 커스텀 예외를 던지는 것이 좋습니다.
            throw new RuntimeException("데이터랩 API 호출에 실패했습니다.");
        }
    }
}
