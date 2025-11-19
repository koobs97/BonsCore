package com.koo.bonscore.common.api.google.service;

import com.koo.bonscore.common.api.google.dto.GoogleFindPlaceResponseDto;
import com.koo.bonscore.common.api.google.dto.GooglePlaceDetailsResponseDto;
import com.koo.bonscore.common.api.google.dto.StoreHoursResponseDto;
import com.koo.bonscore.common.api.google.dto.BusinessStatus; // ★★★ Enum import
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
public class GooglePlacesService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl = "https://maps.googleapis.com/maps/api/place";

    public GooglePlacesService(RestTemplate restTemplate, @Value("${api.google.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    /**
     * 가게 이름과 주소를 기반으로 Google Places API를 호출하여 영업 정보를 반환합니다.
     * @param storeName 가게 이름
     * @param address 주소
     * @param lang 언어 코드
     * @return 상태와 영업시간 정보가 포함된 DTO
     */
    public StoreHoursResponseDto getStoreOpeningHours(String storeName, String address, String lang) {
        String language = Optional.ofNullable(lang).filter(s -> !s.trim().isEmpty()).orElse("ko");
        String query = storeName + " " + address;

        try {
            // Optional 체이닝을 통해 가독성 및 안정성 향상
            // 1. Place ID 찾기 -> 2. Place Details 조회 -> 3. 최종 DTO로 변환
            // 각 단계에서 실패(empty) 시 orElseGet을 통해 적절한 에러 DTO 반환
            return findPlaceId(query, language)
                    .flatMap(placeId -> getPlaceDetails(placeId, language))
                    .map(this::buildResponseFromDetails)
                    .orElseGet(() -> {
                        log.warn("가게를 찾을 수 없거나 상세 정보가 없습니다. query: {}", query);
                        return StoreHoursResponseDto.notFound();
                    });
        } catch (Exception e) {
            log.error("Google Places API 처리 중 예상치 못한 오류 발생. query: {}", query, e);
            return StoreHoursResponseDto.apiError();
        }
    }

    /**
     * Google의 'Find Place' API를 호출하여 Place ID를 Optional로 반환합니다.
     */
    private Optional<String> findPlaceId(String query, String lang) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/findplacefromtext/json")
                .queryParam("input", query)
                .queryParam("inputtype", "textquery")
                .queryParam("fields", "place_id")
                .queryParam("key", apiKey)
                .queryParam("language", lang)
                .build().toUri();

        try {
            GoogleFindPlaceResponseDto response = restTemplate.getForObject(uri, GoogleFindPlaceResponseDto.class);
            return Optional.ofNullable(response)
                    .filter(r -> "OK".equals(r.getStatus()))
                    .map(GoogleFindPlaceResponseDto::getCandidates)
                    .filter(c -> !c.isEmpty())
                    .map(c -> c.get(0).getPlaceId());
        } catch (HttpClientErrorException e) {
            log.error("findPlaceId API 호출 실패. Status: {}, Query: {}", e.getStatusCode(), query);
            return Optional.empty();
        }
    }

    /**
     * Google의 'Place Details' API를 호출하여 상세 정보를 Optional로 반환합니다.
     */
    private Optional<GooglePlaceDetailsResponseDto> getPlaceDetails(String placeId, String lang) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/details/json")
                .queryParam("place_id", placeId)
                .queryParam("fields", "business_status,opening_hours")
                .queryParam("key", apiKey)
                .queryParam("language", lang)
                .build().toUri();

        try {
            GooglePlaceDetailsResponseDto response = restTemplate.getForObject(uri, GooglePlaceDetailsResponseDto.class);
            return Optional.ofNullable(response)
                    .filter(r -> "OK".equals(r.getStatus()));
        } catch (HttpClientErrorException e) {
            log.error("getPlaceDetails API 호출 실패. Status: {}, PlaceID: {}", e.getStatusCode(), placeId);
            return Optional.empty();
        }
    }

    /**
     * API 상세 응답을 바탕으로 최종 StoreHoursResponseDto를 생성합니다.
     */
    private StoreHoursResponseDto buildResponseFromDetails(GooglePlaceDetailsResponseDto details) {
        GooglePlaceDetailsResponseDto.PlaceDetailsResult result = details.getResult();
        if (result == null) {
            return StoreHoursResponseDto.apiError(); // 결과 객체가 없는 비정상 상황
        }

        // 1. 영구 폐업 상태 확인
        if ("CLOSED_PERMANENTLY".equals(result.getBusinessStatus())) {
            return StoreHoursResponseDto.closedPermanently();
        }

        // 2. 영업시간 정보 객체가 아예 없는 경우
        GooglePlaceDetailsResponseDto.OpeningHours openingHours = result.getOpeningHours();
        if (openingHours == null) {
            return StoreHoursResponseDto.noInfo();
        }

        // 3. 정상적으로 영업시간 정보가 있는 경우
        return StoreHoursResponseDto.success(
                openingHours.isOpenNow(),
                result.getBusinessStatus(),
                openingHours.getWeekdayText()
        );
    }
}