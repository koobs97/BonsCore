package com.koo.bonscore.common.api.kakao.recomend.service;

import com.koo.bonscore.common.api.kakao.recomend.dto.RecommendedStoreDto;
import com.koo.bonscore.common.api.kakao.surround.dto.KakaoMapResponse;
import com.koo.bonscore.common.api.kakao.surround.service.KakaoMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreRecommendationService {

    private final KakaoMapService kakaoMapService;

    // 1. 서울을 포함하는 위도, 경도 범위 정의
    private static final double MIN_LATITUDE = 37.42;  // 남쪽
    private static final double MAX_LATITUDE = 37.70;  // 북쪽
    private static final double MIN_LONGITUDE = 126.75; // 서쪽
    private static final double MAX_LONGITUDE = 127.18; // 동쪽

    // 2. 검색할 카테고리 코드 정의 (FD6: 음식점, CE7: 카페)
    private static final List<String> CATEGORY_CODES = Arrays.asList("FD6", "CE7");

    private static final int RECOMMENDATION_COUNT = 6;

    /**
     * 서울 내 음식점 또는 카페를 랜덤으로 4곳 추천합니다.
     */
    public List<RecommendedStoreDto> getRecommendedStores() {
        // 중복 추천을 방지하기 위해 Set 사용
        Set<RecommendedStoreDto> recommendedStores = new HashSet<>();
        int maxRetries = 10; // 무한 루프 방지를 위한 최대 시도 횟수

        // 3. 목표 개수(4개)가 채워질 때까지 반복
        while (recommendedStores.size() < RECOMMENDATION_COUNT && maxRetries > 0) {
            // 랜덤 좌표 생성
            String randomLongitude = String.valueOf(ThreadLocalRandom.current().nextDouble(MIN_LONGITUDE, MAX_LONGITUDE));
            String randomLatitude = String.valueOf(ThreadLocalRandom.current().nextDouble(MIN_LATITUDE, MAX_LATITUDE));

            // 랜덤 카테고리 선택
            String randomCategory = CATEGORY_CODES.get(ThreadLocalRandom.current().nextInt(CATEGORY_CODES.size()));

            // 카카오 API 호출하여 주변 가게 검색 (반경 2km)
            KakaoMapResponse response = kakaoMapService.searchByCategory(randomCategory, randomLongitude, randomLatitude, 2000);

            if (response != null && response.getDocuments() != null && !response.getDocuments().isEmpty()) {
                // 검색된 결과를 DTO로 변환하여 Set에 추가
                response.getDocuments().stream()
                        .map(RecommendedStoreDto::from)
                        .forEach(recommendedStores::add);
            }
            maxRetries--;
        }

        // 4. Set을 List로 변환하고, 4개만 잘라서 반환
        return recommendedStores.stream()
                .limit(RECOMMENDATION_COUNT)
                .collect(Collectors.toList());
    }
}
