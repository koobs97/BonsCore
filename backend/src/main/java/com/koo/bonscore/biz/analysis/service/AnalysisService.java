package com.koo.bonscore.biz.analysis.service;

import com.koo.bonscore.biz.analysis.dto.SearchRequestDto;
import com.koo.bonscore.biz.analysis.dto.StoreAnalysisResultDto;
import com.koo.bonscore.biz.analysis.dto.StoreDetailRequestDto;
import com.koo.bonscore.common.api.google.service.GoogleTranslateService;
import com.koo.bonscore.common.api.kakao.surround.dto.KakaoMapResponse;
import com.koo.bonscore.common.api.kakao.surround.dto.SurroundingDataDto;
import com.koo.bonscore.common.api.kakao.surround.service.KakaoMapService;
import com.koo.bonscore.common.api.naver.NaverApiClient;
import com.koo.bonscore.common.api.naver.dto.NaverApiResponseDto;
import com.koo.bonscore.biz.analysis.dto.SimpleStoreInfoDto;
import com.koo.bonscore.common.api.naver.dto.blog.NaverBlogSearchResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.koo.bonscore.common.api.naver.dto.NaverItemDto;
import org.springframework.web.util.HtmlUtils;

/**
 * <pre>
 * AnalysisService.java
 * 설명 : 웨이팅 예측 분석 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final NaverApiClient naverApiClient;
    private final KakaoMapService kakaoMapService;
    private final GoogleTranslateService googleTranslateService;

    // HTML 태그 제거용 정규표현식
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final String TRANSLATION_SEPARATOR = " ||| ";

    /**
     * Naver 지역 검색 API를 통해 가게를 검색
     * @param query 사용자 검색어
     * @return 가게 리스트 (최대 5건)
     */
    public List<SimpleStoreInfoDto> searchStoresAndAnalyze(SearchRequestDto request) {

        String query = request.getQuery();
        String lang = request.getLang();

        log.info("가게 분석 서비스 시작: query={}", query);

        NaverApiResponseDto naverResponse = naverApiClient.searchLocal(query);

        if (naverResponse == null || naverResponse.getItems() == null) {
            return Collections.emptyList();
        }

        List<NaverItemDto> items = naverResponse.getItems();

        if ("en".equalsIgnoreCase(lang)) {
            // 번역할 한글 가게 이름만 리스트로 추출
            List<String> textsToTranslate = items.stream()
                    .map(item -> {
                        // ★★★ HTML 태그를 먼저 제거하고, 그 다음에 unescape 처리 ★★★
                        String cleanTitle = HTML_TAG_PATTERN.matcher(item.getTitle()).replaceAll("");
                        String originalName = HtmlUtils.htmlUnescape(cleanTitle);

                        String fullAddress = getFullAddress(item);
                        return originalName + TRANSLATION_SEPARATOR + fullAddress;
                    })
                    .collect(Collectors.toList());

            // 2. 구글 번역 API를 통해 리스트 전체를 한번에 번역
            List<String> translatedTexts = googleTranslateService.translateTexts(textsToTranslate, "ko", "en");

            // 3. 번역 실패 시, 원본 한글 데이터로 대체하여 반환 (Fallback)
            if (translatedTexts == null || translatedTexts.size() != items.size()) {
                log.warn("가게 정보 번역에 실패했습니다. 원본 데이터로 대체합니다.");
                return transformToStoreResult(items, false);
            }

            // 4. (성공 시) 원본 데이터와 번역된 데이터를 조합하여 최종 DTO 리스트 생성
            AtomicLong idCounter = new AtomicLong(1);
            return IntStream.range(0, items.size())
                    .mapToObj(i -> {
                        NaverItemDto item = items.get(i);
                        String cleanOriginalTitle = HTML_TAG_PATTERN.matcher(item.getTitle()).replaceAll("");
                        String originalName = HtmlUtils.htmlUnescape(cleanOriginalTitle);
                        String[] originalAddresses = processAddressString(getFullAddress(item)); // 한글 주소 분리

                        // --- 번역된 영문 데이터 추출 및 분리 ---
                        String translatedCombinedText = HtmlUtils.htmlUnescape(translatedTexts.get(i));
                        String[] parts = translatedCombinedText.split(Pattern.quote("|||"));
                        String translatedName = (parts.length > 0) ? parts[0].trim() : originalName;
                        String translatedAddress = (parts.length > 1) ? parts[1].trim() : getFullAddress(item);
                        String[] translatedAddresses = processAddressString(translatedAddress); // 영문 주소 분리

                        // --- 최종 DTO 생성 ---
                        return new SimpleStoreInfoDto(
                                idCounter.getAndIncrement(),
                                translatedName,                 // name (영문)
                                originalName,                   // nameKo (한글)
                                translatedAddresses[0],         // simpleAddress (영문)
                                translatedAddresses[1],         // detailAddress (영문)
                                originalAddresses[0],           // simpleAddressKo (한글)
                                originalAddresses[1]            // detailAddressKo (한글)
                        );
                    })
                    .collect(Collectors.toList());
        }
        else {
            return transformToStoreResult(items, true);
        }
    }

    /**
     * NaverItemDto에서 전체 주소 문자열을 가져오는 헬퍼 메서드
     */
    private String getFullAddress(NaverItemDto item) {
        return (item.getRoadAddress() != null && !item.getRoadAddress().isEmpty())
                ? item.getRoadAddress()
                : item.getAddress();
    }

    /**
     * 전체 주소 문자열을 simpleAddress와 detailAddress로 분리하는 헬퍼 메서드
     * @param fullAddress 전체 주소 문자열 (한글 또는 영문)
     * @return String 배열: [0] = simpleAddress, [1] = detailAddress
     */
    private String[] processAddressString(String fullAddress) {
        String simpleAddress = fullAddress;
        String detailAddress = "";

        if (fullAddress != null && !fullAddress.isEmpty()) {
            String[] addressParts = fullAddress.split(" ");
            if (addressParts.length > 2) {
                // 예: "Seoul Jongno-gu" 또는 "서울 종로구"
                simpleAddress = addressParts[0] + " " + addressParts[1];
                detailAddress = String.join(" ", Arrays.copyOfRange(addressParts, 2, addressParts.length));
            }
        }
        return new String[]{simpleAddress, detailAddress};
    }

    /**
     * 주소 문자열을 simpleAddress와 detailAddress로 분리하는 헬퍼 메서드
     * @param item NaverItemDto
     * @return String 배열: [0] = simpleAddress, [1] = detailAddress
     */
    private String[] processAddress(NaverItemDto item) {
        String fullAddress = (item.getRoadAddress() != null && !item.getRoadAddress().isEmpty())
                ? item.getRoadAddress()
                : item.getAddress();

        String simpleAddress = fullAddress;
        String detailAddress = "";

        if (fullAddress != null && !fullAddress.isEmpty()) {
            String[] addressParts = fullAddress.split(" ");
            if (addressParts.length > 2) {
                simpleAddress = addressParts[0] + " " + addressParts[1];
                detailAddress = String.join(" ", Arrays.copyOfRange(addressParts, 2, addressParts.length));
            }
        }
        return new String[]{simpleAddress, detailAddress};
    }

    /**
     * Naver API로부터 받은 원본 데이터 리스트를 서비스에서 사용할 DTO 리스트로 변환
     * - 제목의 HTML 태그를 제거
     * - 주소를 간단한 주소와 상세 주소로 분리
     * @param items Naver 지역 검색 API 결과 아이템 ({@link NaverItemDto}) 리스트
     * @return 변환된 가게 정보 DTO ({@link SimpleStoreInfoDto}) 리스트
     */
    private List<SimpleStoreInfoDto> transformToStoreResult(List<NaverItemDto> items, boolean isKoreanMode) {
        AtomicLong idCounter = new AtomicLong(1);
        return items.stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    String cleanTitle = HTML_TAG_PATTERN.matcher(item.getTitle()).replaceAll("");
                    String originalName = HtmlUtils.htmlUnescape(cleanTitle);
                    String[] addresses = processAddressString(getFullAddress(item));

                    // isKoreanMode가 true이면 모든 필드에 한글 데이터를 채웁니다.
                    return new SimpleStoreInfoDto(
                            idCounter.getAndIncrement(),
                            originalName,
                            originalName,
                            addresses[0],
                            addresses[1],
                            addresses[0],
                            addresses[1]
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 가게에 대한 상세 분석을 수행하고, 분석 결과를 DTO로 반환
     * 블로그 리뷰 수, 선택된 시간에 따른 점수 등을 계산
     * @param request
     * @return
     */
    public StoreAnalysisResultDto analyzeStoreDetails(StoreDetailRequestDto request) {
        log.info("가게 상세 분석 서비스 시작: storeName={}, selectedTime={}", request.getName(), request.getSelectedTime());

        // 1. 블로그 리뷰 수 조회
        String searchQueryForBlog = request.getSimpleAddress() + " " + request.getName();
        NaverBlogSearchResponseDto blogResponse = naverApiClient.searchBlog(searchQueryForBlog);
        int blogCount = (blogResponse != null) ? blogResponse.getTotal() : 0;

        // 2. 시간/요일 기반 점수 계산
        int timeScore = calculateTimeScore(request.getSelectedTime());

        // 3. 블로그 리뷰 수 기반 점수 계산
        int blogReviewScore = calculateBlogReviewScore(blogCount);

        // 5. 최종 결과 DTO 빌드
        return StoreAnalysisResultDto.builder()
                .name(request.getName())
                .simpleAddress(request.getSimpleAddress())
                .detailAddress(request.getDetailAddress())
                .blogReviewCount(blogCount) // 원본 블로그 리뷰 수
                .timeScore(timeScore)           // 계산된 시간 점수
                .blogReviewScore(blogReviewScore) // 계산된 블로그 점수
                .build();
    }

    /**
     * 시간/요일 기반 점수를 계산하는 메서드
     * @param selectedTime 프론트에서 받은 시간 (예: "16-18")
     * @return 시간/요일 점수
     */
    private int calculateTimeScore(String selectedTime) {
        if (selectedTime == null || selectedTime.isEmpty() || !selectedTime.contains("-")) {
            return 0; // 시간 정보가 없으면 0점 처리
        }

        try {
            LocalDate today = LocalDate.now();
            DayOfWeek dayOfWeek = today.getDayOfWeek();

            // "16-18" -> 16 추출
            int startHour = Integer.parseInt(selectedTime.split("-")[0]);

            // 금요일 또는 토요일 저녁 (18시 이후)
            if ((dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) && startHour >= 18) {
                return 30;
            }
            // 평일 저녁 (18시 이후)
            if (dayOfWeek.getValue() >= 1 && dayOfWeek.getValue() <= 5 && startHour >= 18) {
                return 20;
            }
            // 주말 점심 (12시 ~ 14시)
            if ((dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) && startHour >= 12 && startHour < 14) {
                return 20;
            }
            // 평일 점심 (12시 ~ 14시) - Vue에서는 13시까지였지만 12-14 슬롯이므로 14시 미만으로 처리
            if (dayOfWeek.getValue() >= 1 && dayOfWeek.getValue() <= 5 && startHour >= 12 && startHour < 14) {
                return 15;
            }
            // 애매한 시간 (14시 ~ 17시) - 14-16, 16-18 슬롯이 해당
            if (startHour >= 14 && startHour < 18) {
                return -10;
            }

        } catch (NumberFormatException e) {
            log.error("시간 파싱 오류: selectedTime={}", selectedTime, e);
            return 0;
        }

        return 0; // 그 외 시간은 기본 0점
    }

    /**
     * 블로그 리뷰 수에 따른 점수를 계산하는 메서드
     * @param blogCount 블로그 리뷰 총 개수
     * @return 블로그 점수
     */
    private int calculateBlogReviewScore(int blogCount) {
        if (blogCount >= 1000) {
            return 15;
        } else if (blogCount >= 500) {
            return 10;
        } else if (blogCount >= 100) {
            return 5;
        }
        return 0;
    }

    /**
     * 주변 상권 데이터를 수집하여 DTO로 반환하는 메서드
     */
    public SurroundingDataDto getSurroundingData(StoreDetailRequestDto request) {
        // 1. 요청된 가게의 좌표 및 카테고리 정보 가져오기
        KakaoMapResponse.Document storeInfo = kakaoMapService.searchAndGetFirst(request.getName());
        if (storeInfo == null) {
            log.warn("가게 좌표 정보를 찾을 수 없습니다: {}", request.getName());
            // 데이터가 없으면 모든 카운트를 0으로 설정하여 반환
            return SurroundingDataDto.builder().build();
        }

        String longitude = storeInfo.getX();
        String latitude = storeInfo.getY();
        String storeCategoryCode = storeInfo.getCategoryGroupCode();
        int radius = 500; // 분석 반경 (500m)

        // 2. 주변 '핫플레이스' 데이터 수집 (음식점, 카페, 영화관)
        int hotPlaceCount = 0;
        List<String> hotPlaceCategories = Arrays.asList("FD6", "CE7", "CT1");
        for (String categoryCode : hotPlaceCategories) {
            KakaoMapResponse response = kakaoMapService.searchByCategory(categoryCode, longitude, latitude, radius);
            if (response != null) {
                hotPlaceCount += response.getMeta().getTotalCount();
            }
        }
        log.info("[{}] 주변 핫플레이스 수: {}", request.getName(), hotPlaceCount);

        // 3. 주변 '경쟁 가게' 데이터 수집
        int competitorCount = 0;
        if (storeCategoryCode != null && !storeCategoryCode.isEmpty()) {
            KakaoMapResponse response = kakaoMapService.searchByCategory(storeCategoryCode, longitude, latitude, radius);
            if (response != null) {
                // 검색 결과에는 자기 자신도 포함되므로 1을 빼줌
                competitorCount = Math.max(0, response.getMeta().getTotalCount() - 1);
            }
        }
        log.info("[{}] 주변 경쟁 가게 수: {}", request.getName(), competitorCount);

        // 4. 주변 '주요 시설' 데이터 수집 (역, 대학, 오피스)
        KakaoMapResponse subwayResponse = kakaoMapService.searchByCategory("SW8", longitude, latitude, radius);
        int subwayStationCount = (subwayResponse != null) ? subwayResponse.getMeta().getTotalCount() : 0;

        KakaoMapResponse schoolResponse = kakaoMapService.searchByCategory("SC4", longitude, latitude, radius);
        int universityCount = (schoolResponse != null) ? schoolResponse.getMeta().getTotalCount() : 0;

        // 오피스 빌딩은 '빌딩' 키워드로 검색하여 개수 파악
        KakaoMapResponse officeResponse = kakaoMapService.searchByKeyword("빌딩", longitude, latitude, radius);
        int officeBuildingCount = (officeResponse != null) ? officeResponse.getMeta().getTotalCount() : 0;

        log.info("[{}] 주변 시설: 지하철역({}), 대학교({}), 오피스빌딩({})", request.getName(), subwayStationCount, universityCount, officeBuildingCount);

        // 5. 수집된 데이터를 DTO에 담아 반환
        return SurroundingDataDto.builder()
                .hotPlaceCount(hotPlaceCount)
                .competitorCount(competitorCount)
                .subwayStationCount(subwayStationCount)
                .universityCount(universityCount)
                .officeBuildingCount(officeBuildingCount)
                .build();
    }
}
