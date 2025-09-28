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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/random-recommendations")
    public List<RecommendedStoreDto> getRandomRecommendations() {
        return storeRecommendationService.getRecommendedStores();
    }

    /**
     * 초기 가게 목록 검색 API
     * @param request   검색어
     * @return          초기 가게 검색 목록 (네이버 api 결과 )
     */
    @PostMapping("/stores")
    public List<SimpleStoreInfoDto> searchInitialStores(@RequestBody SearchRequestDto request) {
        return analysisService.searchStoresAndAnalyze(request.getQuery());
    }

    /**
     * 선택된 가게 상세 분석 API
     * @param request   선택한 가게
     * @return          블로그 리뷰 수, 시간/요일 기반 점수 등
     */
    @PostMapping("/details")
    public StoreAnalysisResultDto getStoreDetailsAndAnalysis(@RequestBody StoreDetailRequestDto request) {
        return analysisService.analyzeStoreDetails(request);
    }

    /**
     * 날씨 api
     * @param request   선택한 가게
     * @return          오늘 날씨
     */
    @PostMapping("/weather")
    public WeatherResponseDto getTodayWeather(@RequestBody StoreDetailRequestDto request) {
        return weatherService.getTodayWeather(request.getSimpleAddress());
    }

    /**
     * 오늘 공휴일 여부 확인 API
     * @return 공휴일 정보
     */
    @PostMapping("/holiday-status")
    public HolidayResponseDto getTodayHolidayInfo() {
        return holidayService.getTodayHolidayInfo();
    }

    /**
     * 네이버 데이터랩 검색 트렌드 조회 API
     * @param request   선택한 가게
     * @return          4개월간 검색추이
     */
    @PostMapping("/search-trend")
    public DataLabResponseDto getSearchTrend(@RequestBody SearchRequestDto request) {
        return naverDataLabService.getSearchTrend(request.getQuery());
    }

    /**
     * 가게 영업정보
     * @param request   선택한 가게
     * @return          가게 영업 정보 데이터
     */
    @PostMapping("/openingInfo")
    public StoreHoursResponseDto getStoreOpeningHours(@RequestBody StoreDetailRequestDto request) {
        return googlePlacesService.getStoreOpeningHours(request.getName(), request.getSimpleAddress());
    }

    /**
     * 주변 상권 데이터 분석
     * @param request   선택한 가게
     * @return          주변 상권 데이터
     */
    @PostMapping("/surroundings")
    public SurroundingDataDto getSurroundingData(@RequestBody StoreDetailRequestDto request) {
        return analysisService.getSurroundingData(request);
    }
}
