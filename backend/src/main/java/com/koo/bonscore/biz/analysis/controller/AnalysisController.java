package com.koo.bonscore.biz.analysis.controller;

import com.koo.bonscore.biz.analysis.dto.StoreAnalysisResultDto;
import com.koo.bonscore.biz.analysis.dto.StoreDetailRequestDto;
import com.koo.bonscore.biz.analysis.service.AnalysisService;
import com.koo.bonscore.biz.analysis.dto.SearchRequestDto;
import com.koo.bonscore.biz.analysis.dto.SimpleStoreInfoDto;
import com.koo.bonscore.common.api.google.dto.StoreHoursResponseDto;
import com.koo.bonscore.common.api.google.service.GooglePlacesService;
import com.koo.bonscore.common.api.kakao.recomend.dto.RecommendedStoreDto;
import com.koo.bonscore.common.api.kakao.recomend.service.StoreRecommendationService;
import com.koo.bonscore.common.api.kakao.surround.dto.SurroundingDataDto;
import com.koo.bonscore.common.api.kma.holiday.dto.res.HolidayResponseDto;
import com.koo.bonscore.common.api.kma.holiday.service.HolidayService;
import com.koo.bonscore.common.api.kma.weather.dto.WeatherResponseDto;
import com.koo.bonscore.common.api.kma.weather.service.WeatherService;
import com.koo.bonscore.common.api.naver.NaverDataLabService;
import com.koo.bonscore.common.api.naver.dto.datalab.DataLabResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <pre>
 * AnalysisController.java
 * 설명 : 웨이팅 예측 분석 컨트롤러
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-20
 */
@Tag(name = "Analysis", description = "웨이팅 예측 분석 API (날씨, 공휴일, 검색 트렌드, 주변 상권 등)")
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final StoreRecommendationService storeRecommendationService;
    private final AnalysisService analysisService;
    private final WeatherService weatherService;
    private final HolidayService holidayService;
    private final NaverDataLabService naverDataLabService;
    private final GooglePlacesService googlePlacesService;

    @GetMapping("/random-recommendations")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public List<RecommendedStoreDto> getRandomRecommendations(
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "ko") String lang
    ) {
        return storeRecommendationService.getRecommendedStores(lang);
    }

    /**
     * 초기 가게 목록 검색 API
     * @param request   검색 조건 (query, lang 등)
     * @return          초기 가게 검색 목록 (네이버 api 결과)
     */
    @GetMapping("/stores")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public List<SimpleStoreInfoDto> searchInitialStores(@ModelAttribute SearchRequestDto request) {
        return analysisService.searchStoresAndAnalyze(request);
    }

    /**
     * 선택된 가게 상세 분석 API
     * @param request   선택한 가게 정보
     * @return          블로그 리뷰 수, 시간/요일 기반 점수 등
     */
    @GetMapping("/details")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public StoreAnalysisResultDto getStoreDetailsAndAnalysis(@ModelAttribute StoreDetailRequestDto request) {
        return analysisService.analyzeStoreDetails(request);
    }

    /**
     * 날씨 api
     * @param simpleAddress 가게 주소
     * @return              오늘 날씨
     */
    @GetMapping("/weather")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public WeatherResponseDto getTodayWeather(@RequestParam String simpleAddress) {
        return weatherService.getTodayWeather(simpleAddress);
    }

    /**
     * 오늘 공휴일 여부 확인 API
     * @return 공휴일 정보
     */
    @GetMapping("/holiday-status")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public HolidayResponseDto getTodayHolidayInfo() {
        return holidayService.getTodayHolidayInfo();
    }

    /**
     * 네이버 데이터랩 검색 트렌드 조회 API
     * @param query 검색어
     * @return      4개월간 검색추이
     */
    @GetMapping("/search-trend")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public DataLabResponseDto getSearchTrend(@RequestParam String query) {
        return naverDataLabService.getSearchTrend(query);
    }

    /**
     * 가게 영업정보
     * @param request   선택한 가게 정보
     * @return          가게 영업 정보 데이터
     */
    @GetMapping("/opening-info")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public StoreHoursResponseDto getStoreOpeningHours(@ModelAttribute StoreDetailRequestDto request) {
        return googlePlacesService.getStoreOpeningHours(request.getName(), request.getSimpleAddress(), request.getLang());
    }

    /**
     * 주변 상권 데이터 분석
     * @param request   선택한 가게 정보
     * @return          주변 상권 데이터
     */
    @GetMapping("/surroundings")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public SurroundingDataDto getSurroundingData(@ModelAttribute StoreDetailRequestDto request) {
        return analysisService.getSurroundingData(request);
    }
}
