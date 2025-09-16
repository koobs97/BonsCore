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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/analysis") // 비즈니스 의미에 맞는 URL로 변경
@RequiredArgsConstructor
public class AnalysisController {

    private final StoreRecommendationService storeRecommendationService;

    private final AnalysisService analysisService;
    private final WeatherService weatherService;
    private final HolidayService holidayService;
    private final NaverDataLabService naverDataLabService;
    private final GooglePlacesService googlePlacesService;

    @PostMapping("/random-recommendations")
    public List<RecommendedStoreDto> getRandomRecommendations() {
        return storeRecommendationService.getRecommendedStores();
    }

    /**
     * 1단계: 초기 가게 목록 검색 API (기존 기능)
     */
    @PostMapping("/stores")
    public List<SimpleStoreInfoDto> searchInitialStores(@RequestBody SearchRequestDto request) {
        log.info("초기 가게 목록 검색 요청 수신: query=[{}]", request.getQuery());
        // 기존 searchStoresAndAnalyze 메서드 호출
        return analysisService.searchStoresAndAnalyze(request.getQuery());
    }

    /**
     * 2단계: 선택된 가게 상세 분석 API (새로 추가된 기능)
     */
    @PostMapping("/details")
    public StoreAnalysisResultDto getStoreDetailsAndAnalysis(@RequestBody StoreDetailRequestDto request) {
        log.info("가게 상세 분석 요청 수신: storeName=[{}]", request.getName());
        // 새로 추가된 analyzeStoreDetails 메서드 호출
        return analysisService.analyzeStoreDetails(request);
    }

    /**
     * 3단계: 날씨 api
     */
    @PostMapping("/weather")
    public WeatherResponseDto getTodayWeather(@RequestBody StoreDetailRequestDto request) {
        return weatherService.getTodayWeather(request.getSimpleAddress());
    }

    /**
     * 4단계: 오늘 공휴일 여부 확인 API
     */
    @PostMapping("/holiday-status")
    public HolidayResponseDto getTodayHolidayInfo() {
        return holidayService.getTodayHolidayInfo();
    }

    /**
     * 5단계: 네이버 데이터랩 검색 트렌드 조회 API
     */
    @PostMapping("/search-trend")
    public DataLabResponseDto getSearchTrend(@RequestBody SearchRequestDto request) {
        log.info("네이버 데이터랩 검색 트렌드 조회 요청: query=[{}]", request.getQuery());
        return naverDataLabService.getSearchTrend(request.getQuery());
    }

    @PostMapping("/openingInfo")
    public StoreHoursResponseDto getStoreOpeningHours(@RequestBody StoreDetailRequestDto request) {
        return googlePlacesService.getStoreOpeningHours(request.getName(), request.getSimpleAddress());
    }

    @PostMapping("/surroundings")
    public SurroundingDataDto getSurroundingData(@RequestBody StoreDetailRequestDto request) {
        log.info("주변 상권 데이터 조회 요청: storeName=[{}]", request.getName());
        return analysisService.getSurroundingData(request);
    }
}
