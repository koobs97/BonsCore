package com.koo.bonscore.common.api.kakao.recomend.service;

import org.springframework.web.util.HtmlUtils;
import com.koo.bonscore.common.api.google.service.GoogleTranslateService;
import com.koo.bonscore.common.api.kakao.recomend.dto.RecommendedStoreDto;
import com.koo.bonscore.common.api.kakao.surround.dto.KakaoMapResponse;
import com.koo.bonscore.common.api.kakao.surround.service.KakaoMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreRecommendationService {

    private final KakaoMapService kakaoMapService;
    private final GoogleTranslateService googleTranslateService;

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
    public List<RecommendedStoreDto> getRecommendedStores(String lang) {
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

        List<RecommendedStoreDto> resultList = recommendedStores.stream()
                .limit(RECOMMENDATION_COUNT)
                .toList();

        if ("en".equalsIgnoreCase(lang) && !resultList.isEmpty()) {

            // 특수 구분자 정의 (가게 이름이나 카테고리에 없을 만한 문자)
            final String SEPARATOR = " ||| ";

            // 1. 이름과 카테고리를 구분자와 함께 합쳐서 리스트 생성
            List<String> textsToTranslate = resultList.stream()
                    .map(dto -> dto.getName() + SEPARATOR + dto.getCategory())
                    .collect(Collectors.toList());

            // 2. 합쳐진 텍스트 리스트를 번역 서비스로 전달
            List<String> translatedTexts = googleTranslateService.translateTexts(textsToTranslate, "ko", "en");

            // 3. 번역된 결과를 다시 분리하여 DTO에 각각 설정
            if (translatedTexts != null && translatedTexts.size() == resultList.size()) {
                for (int i = 0; i < resultList.size(); i++) {
                    // 번역된 원본 텍스트
                    String rawTranslatedText = translatedTexts.get(i);

                    // HTML 엔티티를 일반 문자로 변환
                    String translatedText = HtmlUtils.htmlUnescape(rawTranslatedText);

                    // 구분자를 기준으로 번역된 문자열을 분리 (정규식 이스케이프 필요)
                    String[] parts = translatedText.split("\\s*\\|\\|\\|\\s*");

                    if (parts.length == 2) {
                        // 분리된 결과를 각각 name과 category에 설정
                        resultList.get(i).setNameKo(resultList.get(i).getName());
                        resultList.get(i).setName(parts[0].trim());
                        resultList.get(i).setCategory(parts[1].trim());
                    } else {
                        log.warn("번역 결과 분리 실패: '{}'. 원본 데이터를 유지합니다.", translatedText);
                        // 분리에 실패하면 원본 한국어 이름만 영어로 덮어쓰는 등 예외처리 가능
                        resultList.get(i).setName(translatedText.replace(SEPARATOR, " - "));
                    }
                }
            } else {
                log.warn("가게 정보 번역에 실패했거나 결과 개수가 일치하지 않습니다. 원본 데이터를 반환합니다.");
            }
        }

        // 4. Set을 List로 변환하고, 4개만 잘라서 반환
        return recommendedStores.stream()
                .limit(RECOMMENDATION_COUNT)
                .collect(Collectors.toList());
    }
}
