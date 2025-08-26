package com.koo.bonscore.common.api.weather.service;


import com.koo.bonscore.common.api.weather.config.GpsTransfer;
import com.koo.bonscore.common.api.weather.dto.KakaoApiDto;
import com.koo.bonscore.common.api.weather.dto.KmaApiDto;
import com.koo.bonscore.common.api.weather.dto.WeatherResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WebClient.Builder webClientBuilder;

    @Value("${api.kma.key}")
    private String kmaServiceKey;
    @Value("${api.kma.url}")
    private String kmaBaseUrl;
    @Value("${api.kakao.key}")
    private String kakaoRestApiKey;
    @Value("${api.kakao.url}")
    private String kakaoBaseUrl;

    public WeatherResponseDto getTodayWeather(String address) {
        // 1. 주소 -> 위경도 변환
        KakaoApiDto.Document coordinate = getCoordinate(address);
        double lat = Double.parseDouble(coordinate.getY());
        double lon = Double.parseDouble(coordinate.getX());

        // 2. 위경도 -> 기상청 격자 x, y좌표 변환
        GpsTransfer gpsTransfer = new GpsTransfer(lat, lon);
        int nx = gpsTransfer.getNx();
        int ny = gpsTransfer.getNy();

        // 3. 기상청 단기예보 API 호출
        KmaApiDto kmaApiResponse = callKmaApi(nx, ny);

        // 4. API 응답에서 필요한 데이터만 파싱하여 DTO로 변환
        return parseKmaResponse(kmaApiResponse);
    }

    // 1. 주소를 좌표로 변환 (카카오 API)
    private KakaoApiDto.Document getCoordinate(String address) {
        WebClient webClient = webClientBuilder.baseUrl(kakaoBaseUrl).build();

        KakaoApiDto response = webClient.get()
                .uri(uriBuilder -> uriBuilder.queryParam("query", address).build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestApiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(KakaoApiDto.class)
                .block(); // 동기 방식

        return Objects.requireNonNull(response).getDocuments().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("주소에 해당하는 좌표를 찾을 수 없습니다: " + address));
    }

    // 3. 기상청 API 호출
    private KmaApiDto callKmaApi(int nx, int ny) {
        WebClient webClient = webClientBuilder.baseUrl(kmaBaseUrl).build();

        // 오늘 날짜와 현재 시간 기준으로 base_date, base_time 계산
        Map<String, String> baseDateTime = getBaseDateTime();
        String baseDate = baseDateTime.get("baseDate");
        String baseTime = baseDateTime.get("baseTime");

        // numOfRows를 넉넉하게 설정하여 현재 시간대의 모든 카테고리 데이터를 한번에 가져옴
        String uri = UriComponentsBuilder.fromHttpUrl(kmaBaseUrl)
                .queryParam("serviceKey", kmaServiceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1000) // 충분한 개수
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true)
                .toUriString();

        return webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(KmaApiDto.class)
                .block();
    }

    // 4. 기상청 응답 파싱
    private WeatherResponseDto parseKmaResponse(KmaApiDto kmaApiResponse) {
        if (kmaApiResponse == null || kmaApiResponse.getResponse() == null ||
                kmaApiResponse.getResponse().getBody() == null || kmaApiResponse.getResponse().getBody().getItems() == null) {
            throw new RuntimeException("기상청 API 응답이 올바르지 않습니다.");
        }

        Map<String, String> weatherData = new HashMap<>();

        // 현재 시간과 가장 가까운 예보 시간을 찾기 위함
        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH00"));

        for (KmaApiDto.Item item : kmaApiResponse.getResponse().getBody().getItems().getItem()) {
            if (item.getFcstTime().equals(now)) {
                weatherData.put(item.getCategory(), item.getFcstValue());
            }
        }

        return WeatherResponseDto.builder()
                .temperature(weatherData.getOrDefault("TMP", "N/A"))
                .sky(mapSkyStatus(weatherData.getOrDefault("SKY", "N/A")))
                .precipitation(mapPrecipitationStatus(weatherData.getOrDefault("PTY", "N/A")))
                .build();
    }

    // 기상청 API는 특정 시간에만 데이터를 제공하므로, 현재 시간에 맞는 base_date, base_time을 계산해야 함
    private Map<String, String> getBaseDateTime() {
        Map<String, String> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // 기상청 API 제공 시간: 0200, 0500, 0800, 1100, 1400, 1700, 2000, 2300
        LocalTime baseTime = LocalTime.of(2, 0);
        if (now.toLocalTime().isBefore(baseTime.plusMinutes(10))) {
            // 현재 시간이 02:10 이전이면, 전날 23시 데이터를 사용
            now = now.minusDays(1);
            baseTime = LocalTime.of(23, 0);
        } else {
            if (now.toLocalTime().isBefore(LocalTime.of(5, 10))) baseTime = LocalTime.of(2, 0);
            else if (now.toLocalTime().isBefore(LocalTime.of(8, 10))) baseTime = LocalTime.of(5, 0);
            else if (now.toLocalTime().isBefore(LocalTime.of(11, 10))) baseTime = LocalTime.of(8, 0);
            else if (now.toLocalTime().isBefore(LocalTime.of(14, 10))) baseTime = LocalTime.of(11, 0);
            else if (now.toLocalTime().isBefore(LocalTime.of(17, 10))) baseTime = LocalTime.of(14, 0);
            else if (now.toLocalTime().isBefore(LocalTime.of(20, 10))) baseTime = LocalTime.of(17, 0);
            else if (now.toLocalTime().isBefore(LocalTime.of(23, 10))) baseTime = LocalTime.of(20, 0);
            else baseTime = LocalTime.of(23, 0);
        }

        result.put("baseDate", now.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        result.put("baseTime", baseTime.format(DateTimeFormatter.ofPattern("HHmm")));

        return result;
    }

    // 하늘 상태 코드 -> 한글 변환
    private String mapSkyStatus(String skyCode) {
        return switch (skyCode) {
            case "1" -> "맑음";
            case "3" -> "구름많음";
            case "4" -> "흐림";
            default -> "정보 없음";
        };
    }

    // 강수 형태 코드 -> 한글 변환
    private String mapPrecipitationStatus(String ptyCode) {
        return switch (ptyCode) {
            case "0" -> "없음";
            case "1" -> "비";
            case "2" -> "비/눈";
            case "3" -> "눈";
            case "4" -> "소나기";
            default -> "정보 없음";
        };
    }
}
