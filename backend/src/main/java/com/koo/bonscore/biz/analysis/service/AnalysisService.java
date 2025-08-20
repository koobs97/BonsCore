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
        log.info("가게 상세 분석 서비스 시작: storeName={}", request.getName());

        // 가게 이름과 주소를 조합하여 검색 정확도를 높입니다.
        String searchQueryForBlog = request.getSimpleAddress() + " " + request.getName();
        NaverBlogSearchResponseDto blogResponse = naverApiClient.searchBlog(searchQueryForBlog);

        int blogCount = (blogResponse != null) ? blogResponse.getTotal() : 0;

        // 향후 이 곳에서 날씨 조회 등 다른 분석 로직을 추가하고 결과를 조합합니다.

        return StoreAnalysisResultDto.builder()
                .name(request.getName())
                .simpleAddress(request.getSimpleAddress())
                .detailAddress(request.getDetailAddress())
                .blogReviewCount(blogCount)
                .build();
    }
}
