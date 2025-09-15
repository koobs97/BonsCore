package com.koo.bonscore.common.api.kakao.service;

import com.koo.bonscore.common.api.kakao.dto.KakaoMapResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMapService {

    private final RestTemplate restTemplate;

    @Value("${api.kakao.key}")
    private String kakaoApiKey;

    private static final String KAKAO_CATEGORY_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/category.json";
    private static final String KAKAO_KEYWORD_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";


    public KakaoMapResponse.Document searchAndGetFirst(String query) {
        // 기존 searchByKeyword(String, int) 대신 새로운 메서드를 호출합니다.
        KakaoMapResponse response = searchByKeyword(query);
        if (response != null && !response.getDocuments().isEmpty()) {
            return response.getDocuments().get(0);
        }
        return null;
    }

    // ★★★★★ 핵심 수정 부분 1: 키워드 검색 메서드 변경 ★★★★★
    /**
     * 키워드로 장소 검색 (RestTemplate이 직접 인코딩하도록 변경)
     */
    public KakaoMapResponse searchByKeyword(String query) {
        log.info("카카오 키워드 검색 요청: query='{}'", query);

        // 요청 헤더 준비
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // URL 템플릿 정의
        String urlTemplate = KAKAO_KEYWORD_SEARCH_URL + "?query={query}&size=1";

        try {
            // RestTemplate.exchange에 URL 템플릿과 파라미터(query)를 직접 전달
            return restTemplate.exchange(urlTemplate, HttpMethod.GET, entity, KakaoMapResponse.class, query).getBody();
        } catch (Exception e) {
            log.error("Kakao API 요청 실패: {}", e.getMessage());
            return null;
        }
    }

    // ★★★★★ 핵심 수정 부분 2: 주변 검색 메서드도 동일한 방식으로 변경 ★★★★★
    /**
     * 특정 좌표 주변의 키워드별 장소 검색 (RestTemplate 인코딩 방식 적용)
     */
    public KakaoMapResponse searchByKeyword(String keyword, String longitude, String latitude, int radius) {
        log.info("카카오 주변 키워드 검색 요청: keyword='{}', lon={}, lat={}, radius={}", keyword, longitude, latitude, radius);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String urlTemplate = KAKAO_KEYWORD_SEARCH_URL + "?query={keyword}&x={x}&y={y}&radius={radius}&size=15";

        try {
            return restTemplate.exchange(urlTemplate, HttpMethod.GET, entity, KakaoMapResponse.class, keyword, longitude, latitude, radius).getBody();
        } catch (Exception e) {
            log.error("Kakao API 요청 실패: {}", e.getMessage());
            return null;
        }
    }

    // 카테고리 검색도 동일하게 수정해주면 좋습니다.
    public KakaoMapResponse searchByCategory(String categoryCode, String longitude, String latitude, int radius) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String urlTemplate = KAKAO_CATEGORY_SEARCH_URL + "?category_group_code={category_group_code}&x={x}&y={y}&radius={radius}&size=15";

        try {
            return restTemplate.exchange(urlTemplate, HttpMethod.GET, entity, KakaoMapResponse.class, categoryCode, longitude, latitude, radius).getBody();
        } catch (Exception e) {
            log.error("Kakao API 요청 실패: {}", e.getMessage());
            return null;
        }
    }
}
