package com.koo.bonscore.common.api.kma.subway;

import com.koo.bonscore.biz.analysis.dto.StoreDetailRequestDto;
import com.koo.bonscore.common.api.kma.weather.dto.KakaoApiDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubwayAnalysisService {

    private final WebClient.Builder webClientBuilder;

    @Value("${api.kakao.key}")
    private String kakaoRestApiKey;
    @Value("${api.kakao.url}")
    private String kakaoAddressUrl;
    @Value("${api.kakao.category-url}")
    private String kakaoCategoryUrl;
    @Value("${api.seoul.key}")
    private String seoulApiKey;
    @Value("${api.seoul.subway-url}")
    private String seoulSubwayUrl;

    /**
     * 메인 로직: 주소와 시간을 받아 지하철 혼잡도 분석
     */
    public SubwayCongestionDto analyzeSubwayCongestion(StoreDetailRequestDto request) {
        // 서울 지역이 아니면 분석을 건너뜀
        if (!request.getSimpleAddress().contains("서울")) {
            log.info("분석 지역이 서울이 아니므로 지하철 혼잡도 분석을 건너뜁니다: {}", request.getSimpleAddress());
            return SubwayCongestionDto.builder().available(false).build();
        }

        try {
            // 1. 주소 -> 위경도 변환 (기존 WeatherService 로직 재활용)
            KakaoApiDto.Document coordinate = getCoordinate(request.getSimpleAddress());
            double lat = Double.parseDouble(coordinate.getY());
            double lon = Double.parseDouble(coordinate.getX());

            // 2. 위경도 -> 가장 가까운 지하철역 정보 찾기
            KakaoCategoryApiDto.Document nearestStation = findNearestSubwayStation(lat, lon);
            String stationFullName = nearestStation.getPlaceName(); // "강남역"
            String stationQueryName = stationFullName.replace("역", ""); // "강남" (API 쿼리용)
            String lineNum = extractLineNumber(nearestStation.getCategoryName()); // "2호선"

            // 3. 서울시 API로 승하차 인원 데이터 조회
            String useMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
            SeoulSubwayApiDto.Row subwayData = getSubwayPassengerData(lineNum, stationQueryName, useMonth);

            // 4. 선택된 시간대의 총 승하차 인원 계산
            int startHour = Integer.parseInt(request.getSelectedTime().split("-")[0]);
            long totalPassengers = calculateTotalPassengersForTimeSlot(subwayData, startHour);
            log.info("[{}] {}시~{}시 총 인원: {}", stationFullName, startHour, startHour + 2, totalPassengers);

            // 5. 혼잡도 점수 계산
            return calculateCongestionScore(stationFullName, totalPassengers);

        } catch (Exception e) {
            log.error("지하철 혼잡도 분석 중 오류 발생: {}", e.getMessage(), e);
            return SubwayCongestionDto.builder().available(false).build();
        }
    }

    /**
     * 1. 주소를 좌표로 변환 (카카오 주소 검색 API)
     * WeatherService와 중복되므로, 실제 프로젝트에서는 KakaoApiService 등으로 분리하는 것이 좋습니다.
     */
    private KakaoApiDto.Document getCoordinate(String address) {
        WebClient webClient = webClientBuilder.baseUrl(kakaoAddressUrl).build();
        KakaoApiDto response = webClient.get()
                .uri(uriBuilder -> uriBuilder.queryParam("query", address).build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestApiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(KakaoApiDto.class)
                .block();

        return Objects.requireNonNull(response).getDocuments().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("주소에 해당하는 좌표를 찾을 수 없습니다: " + address));
    }

    /**
     * 2. 위경도 기준 가장 가까운 지하철역 찾기 (카카오 카테고리 검색 API)
     */
    private KakaoCategoryApiDto.Document findNearestSubwayStation(double lat, double lon) {
        WebClient webClient = webClientBuilder.baseUrl(kakaoCategoryUrl).build();
        KakaoCategoryApiDto response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("category_group_code", "SW8") // 지하철역 코드
                        .queryParam("x", lon)
                        .queryParam("y", lat)
                        .queryParam("radius", 1000) // 반경 1km 내
                        .queryParam("sort", "distance") // 거리순 정렬
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestApiKey)
                .retrieve()
                .bodyToMono(KakaoCategoryApiDto.class)
                .block();

        return Objects.requireNonNull(response).getDocuments().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("반경 1km 내에 지하철역을 찾을 수 없습니다."));
    }

    /**
     * 3. 서울 열린데이터 광장 API 호출하여 특정 역의 월간 시간대별 데이터 조회
     */
    private SeoulSubwayApiDto.Row getSubwayPassengerData(String lineNum, String stationName, String useMonth) {
        // ★★★ [수정] WeatherService와 동일한 방식으로 UriComponentsBuilder 사용 ★★★
        String fullUrl = UriComponentsBuilder.fromHttpUrl(seoulSubwayUrl)
                .path("/{key}/json/CardSubwayTime/1/100/{useMonth}/{lineNum}/{stationName}")
                .buildAndExpand(seoulApiKey, useMonth, lineNum, stationName)
                .toUriString();

        // baseUrl을 사용하지 않고, 독립적인 WebClient 인스턴스로 직접 호출
        WebClient webClient = WebClient.create();

        SeoulSubwayApiDto response = webClient.get()
                .uri(fullUrl) // ★★★ 완전한 URL을 전달
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> Mono.error(new RuntimeException("서울시 API 호출 실패: " + clientResponse.statusCode())))
                .bodyToMono(SeoulSubwayApiDto.class)
                .block();

        if (response == null || response.getCardSubwayTime() == null || response.getCardSubwayTime().getRow() == null || response.getCardSubwayTime().getRow().isEmpty()) {
            throw new IllegalStateException("해당 역의 지하철 데이터를 찾을 수 없습니다: " + lineNum + " " + stationName);
        }
        return response.getCardSubwayTime().getRow().get(0);
    }

    /**
     * 4. 사용자가 선택한 시간대 (2시간)에 해당하는 총 승하차 인원 합산
     */
    private long calculateTotalPassengersForTimeSlot(SeoulSubwayApiDto.Row data, int startHour) {
        double ride = 0;
        double alight = 0;
        switch (startHour) {
            case 10:
                ride = data.getTenRide() + data.getElevenRide();
                alight = data.getTenAlight() + data.getElevenAlight();
                break;
            case 12:
                ride = data.getTwelveRide() + data.getThirteenRide();
                alight = data.getTwelveAlight() + data.getThirteenAlight();
                break;
            case 14:
                ride = data.getFourteenRide() + data.getFifteenRide();
                alight = data.getFourteenAlight() + data.getFifteenAlight();
                break;
            case 16:
                ride = data.getSixteenRide() + data.getSeventeenRide();
                alight = data.getSixteenAlight() + data.getSeventeenAlight();
                break;
            case 18:
                ride = data.getEighteenRide() + data.getNineteenRide();
                alight = data.getEighteenAlight() + data.getNineteenAlight();
                break;
            case 20:
                ride = data.getTwentyRide() + data.getTwentyOneRide();
                alight = data.getTwentyAlight() + data.getTwentyOneAlight();
                break;
            default:
                // 요청 시간대가 범위 밖일 경우 0을 반환
                return 0L;
        }
        return (long) (ride + alight);
    }

    /**
     * 5. 총 승하차 인원 기반으로 혼잡도 레벨과 점수 부여
     */
    private SubwayCongestionDto calculateCongestionScore(String stationName, long passengers) {
        String level;
        int score;

        if (passengers > 150000) { level = "매우 혼잡"; score = 25; }
        else if (passengers > 100000) { level = "혼잡"; score = 15; }
        else if (passengers > 50000) { level = "보통"; score = 8; }
        else if (passengers > 20000) { level = "약간 혼잡"; score = 3; }
        else if (passengers > 5000) { level = "여유"; score = 0; }
        else { level = "한산"; score = -5; }

        return SubwayCongestionDto.builder()
                .available(true).stationName(stationName).congestionLevel(level).score(score).build();
    }

    /**
     * 카테고리 문자열에서 'N호선' 정보 추출하는 헬퍼 메서드
     * 예: "교통,수송 > 지하철,전철 > 수도권2호선" -> "2호선"
     */
    private String extractLineNumber(String categoryName) {
        Pattern pattern = Pattern.compile("(\\d+)호선");
        Matcher matcher = pattern.matcher(categoryName);
        if (matcher.find()) {
            return matcher.group(0); // "2호선"
        }
        // 경의중앙선, 공항철도 등 숫자 호선이 아닌 경우에 대한 예외처리
        // 여기서는 간단히 "기타노선" 등으로 처리하거나 예외를 발생시킬 수 있습니다.
        throw new IllegalArgumentException("카테고리에서 호선 정보를 추출할 수 없습니다: " + categoryName);
    }
}
