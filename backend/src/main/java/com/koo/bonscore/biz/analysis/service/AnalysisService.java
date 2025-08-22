package com.koo.bonscore.biz.analysis.service;

import com.koo.bonscore.biz.analysis.dto.StoreAnalysisResultDto;
import com.koo.bonscore.biz.analysis.dto.StoreDetailRequestDto;
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
}
