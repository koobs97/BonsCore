package com.koo.bonscore.biz.analysis.controller;

import com.koo.bonscore.biz.analysis.dto.StoreAnalysisResultDto;
import com.koo.bonscore.biz.analysis.dto.StoreDetailRequestDto;
import com.koo.bonscore.biz.analysis.service.AnalysisService;
import com.koo.bonscore.biz.analysis.dto.SearchRequestDto;
import com.koo.bonscore.biz.analysis.dto.SimpleStoreInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final AnalysisService analysisService;

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
}
