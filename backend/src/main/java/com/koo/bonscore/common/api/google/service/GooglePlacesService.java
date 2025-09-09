package com.koo.bonscore.common.api.google.service;

import com.koo.bonscore.common.api.google.dto.GoogleFindPlaceResponseDto;
import com.koo.bonscore.common.api.google.dto.GooglePlaceDetailsResponseDto;
import com.koo.bonscore.common.api.google.dto.StoreHoursResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

    // WebClient 대신 RestTemplate 주입
    public GooglePlacesService(RestTemplate restTemplate, @Value("${api.google.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    // Mono<String> -> String
    private String findPlaceId(String storeName, String address) {
        String query = storeName + " " + address;
        log.info("Google Find Place API 호출 (동기): query=[{}]", query);

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/findplacefromtext/json")
                .queryParam("input", query)
                .queryParam("inputtype", "textquery")
                .queryParam("fields", "place_id")
                .queryParam("key", apiKey)
                .queryParam("language", "ko")
                .build().toUri();

        try {
            GoogleFindPlaceResponseDto response = restTemplate.getForObject(uri, GoogleFindPlaceResponseDto.class);

            if (response != null && "OK".equals(response.getStatus()) && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                String placeId = response.getCandidates().get(0).getPlaceId();
                log.info("Place ID 찾음: [{}]", placeId);
                return placeId;
            }
            log.warn("Place ID를 찾을 수 없음: query=[{}], status=[{}]", query, response != null ? response.getStatus() : "null");
            return null;
        } catch (Exception e) {
            log.error("findPlaceId API 호출 중 에러 발생", e);
            return null;
        }
    }

    // Mono<GooglePlaceDetailsResponseDto> -> GooglePlaceDetailsResponseDto
    private GooglePlaceDetailsResponseDto getPlaceDetails(String placeId) {
        log.info("Google Place Details API 호출 (동기): placeId=[{}]", placeId);

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/details/json")
                .queryParam("place_id", placeId)
                .queryParam("fields", "business_status,opening_hours")
                .queryParam("key", apiKey)
                .queryParam("language", "ko")
                .build().toUri();

        try {
            return restTemplate.getForObject(uri, GooglePlaceDetailsResponseDto.class);
        } catch (Exception e) {
            log.error("getPlaceDetails API 호출 중 에러 발생", e);
            return null; // 에러 발생 시 null 반환
        }
    }

    // Mono<StoreHoursResponseDto> -> StoreHoursResponseDto
    public StoreHoursResponseDto getStoreOpeningHours(String storeName, String address) {
        // 1. placeId를 동기적으로 조회
        String placeId = findPlaceId(storeName, address);

        // 2. placeId가 없으면 NOT_FOUND 응답 반환
        if (placeId == null) {
            return createNotFoundResponse();
        }

        // 3. placeDetails를 동기적으로 조회
        GooglePlaceDetailsResponseDto detailsResponse = getPlaceDetails(placeId);

        // 4. details 조회가 실패(null)하면 ERROR 응답 반환
        if (detailsResponse == null) {
            return createErrorResponse();
        }

        // 5. 조회 결과를 최종 DTO로 변환하여 반환
        return convertToStoreHoursResponse(detailsResponse);
    }

    // DTO 변환 로직 (동일)
    private StoreHoursResponseDto convertToStoreHoursResponse(GooglePlaceDetailsResponseDto detailsResponse) {
        if ("OK".equals(detailsResponse.getStatus()) && detailsResponse.getResult() != null) {
            GooglePlaceDetailsResponseDto.PlaceDetailsResult result = detailsResponse.getResult();
            boolean isOpen = Optional.ofNullable(result.getOpeningHours())
                    .map(GooglePlaceDetailsResponseDto.OpeningHours::isOpenNow)
                    .orElse(false);

            return StoreHoursResponseDto.builder()
                    .open(isOpen)
                    .businessStatus(result.getBusinessStatus())
                    .weekdayText(Optional.ofNullable(result.getOpeningHours())
                            .map(GooglePlaceDetailsResponseDto.OpeningHours::getWeekdayText)
                            .orElse(Collections.singletonList("영업 시간 정보 없음")))
                    .build();
        }
        return createErrorResponse(); // OK가 아니거나 result가 null일 때
    }

    // 응답 DTO 생성 메서드 (동일)
    private StoreHoursResponseDto createNotFoundResponse() {
        return StoreHoursResponseDto.builder()
                .open(true)
                .businessStatus("NOT_FOUND")
                .weekdayText(Collections.singletonList("영업 정보를 찾을 수 없습니다."))
                .build();
    }

    private StoreHoursResponseDto createErrorResponse() {
        return StoreHoursResponseDto.builder()
                .open(true)
                .businessStatus("DETAILS_API_ERROR")
                .weekdayText(Collections.singletonList("영업 정보를 가져오는데 실패했습니다."))
                .build();
    }
}