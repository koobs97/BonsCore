package com.koo.bonscore.biz.analysis.service;

import com.koo.bonscore.biz.analysis.dto.StoreAnalysisResultDto;
import com.koo.bonscore.biz.analysis.dto.StoreDetailRequestDto;
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

import com.koo.bonscore.common.api.naver.dto.NaverItemDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final NaverApiClient naverApiClient;
    // private final WeatherApiClient weatherApiClient; // 예: 날씨 API 클라이언트 추가
    private final KakaoMapService kakaoMapService;

    // HTML 태그 제거용 정규표현식
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    public List<SimpleStoreInfoDto> searchStoresAndAnalyze(String query) {
        log.info("가게 분석 서비스 시작: query={}", query);

        // 1. Naver API를 통해 가게 정보를 조회합니다. (외부 API 호출 위임)
        NaverApiResponseDto naverResponse = naverApiClient.searchLocal(query);

        if (naverResponse == null || naverResponse.getItems() == null) {
            return Collections.emptyList();
        }

        // 2. 받아온 원본 데이터를 우리 서비스의 DTO로 가공합니다.
        // 이 부분에 모든 비즈니스 로직(주소 가공 등)이 포함됩니다.
        return transformToStoreResult(naverResponse.getItems());
    }

    // NaverItemDto 리스트를 StoreSearchResultDto 리스트로 변환하는 로직 (메서드 분리)
    private List<SimpleStoreInfoDto> transformToStoreResult(List<NaverItemDto> items) {
        AtomicLong idCounter = new AtomicLong(1);

        return items.stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    String cleanTitle = HTML_TAG_PATTERN.matcher(item.getTitle()).replaceAll("");

                    // 주소 가공 로직
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

                    // 향후 이 부분에서 추가 분석 로직 수행 가능
                    // 예: naverItem.getTitle()로 블로그 개수 조회 후 점수 매기기
                    // int blogScore = countBlogs(naverItem.getTitle());

                    return new SimpleStoreInfoDto(
                            idCounter.getAndIncrement(),
                            cleanTitle,
                            simpleAddress,
                            detailAddress
                    );
                })
                .collect(Collectors.toList());
    }

    public StoreAnalysisResultDto analyzeStoreDetails(StoreDetailRequestDto request) {
        log.info("가게 상세 분석 서비스 시작: storeName={}, selectedTime={}", request.getName(), request.getSelectedTime());

        // 1. 블로그 리뷰 수 조회
        String searchQueryForBlog = request.getSimpleAddress() + " " + request.getName();
        NaverBlogSearchResponseDto blogResponse = naverApiClient.searchBlog(searchQueryForBlog);
        int blogCount = (blogResponse != null) ? blogResponse.getTotal() : 0;

        // 2. ★★★ 시간/요일 기반 점수 계산 ★★★
        int timeScore = calculateTimeScore(request.getSelectedTime());

        // 3. ★★★ 블로그 리뷰 수 기반 점수 계산 ★★★
        int blogReviewScore = calculateBlogReviewScore(blogCount);

        // 4. 향후 날씨, SNS, 지도 혼잡도 등 다른 분석 로직 추가...

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
