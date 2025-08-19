package com.koo.bonscore.common.api.naver;

import com.koo.bonscore.core.config.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/search/naver")
public class NaverSearchController {

    private final NaverProperties naverProperties;

    // HTML 태그 제거용 정규표현식
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    // 생성자를 통해 주입
    public NaverSearchController(NaverProperties naverProperties) {
        this.naverProperties = naverProperties;
    }

    @PostMapping("/blog")
    public List<StoreSearchResultDto> searchBlog(@RequestBody SearchRequestDto request) {

        log.info("### 실제로 받은 query 값: [{}]", request.getQuery());

        // 네이버 API의 URI 생성
        URI uri = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com")
                .path("/v1/search/local.json")
                .queryParam("query", request.getQuery())
                .queryParam("display", 5) // 한 번에 표시할 검색 결과 개수
                .queryParam("start", 1)    // 검색 시작 위치
                .queryParam("sort", "random") // 정렬 옵션 (sim: 정확도순, date: 날짜순)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        // RestTemplate을 사용하여 API 호출
        RestTemplate restTemplate = new RestTemplate();

        // 요청 헤더 설정
        RequestEntity<Void> req = RequestEntity
                .get(uri)
                .header("X-Naver-Client-Id", naverProperties.getId())
                .header("X-Naver-Client-Secret", naverProperties.getSecret())
                .build();

        ResponseEntity<NaverApiResponseDto> result = restTemplate.exchange(req, NaverApiResponseDto.class);
        log.info("result.getBody() -> {}", result.getBody());

        NaverApiResponseDto naverResponse = result.getBody();

        List<StoreSearchResultDto> storeList;
        if (naverResponse == null || naverResponse.getItems() == null) {
            storeList = Collections.emptyList();
        } else {
            AtomicLong idCounter = new AtomicLong(1);
            storeList = naverResponse.getItems().stream()
                    .filter(Objects::nonNull)
                    .map(item -> {
                        // ★★★★★★★★★★★ 주소 가공 로직 시작 ★★★★★★★★★★★

                        // 1. 도로명 주소를 우선적으로 사용합니다.
                        String fullAddress = (item.getRoadAddress() != null && !item.getRoadAddress().isEmpty())
                                ? item.getRoadAddress()
                                : item.getAddress();

                        String simpleAddress = fullAddress; // 기본값은 전체 주소
                        String detailAddress = "";

                        // 2. 주소를 공백 기준으로 나눕니다. (예: ["경기도", "안양시", "동안구", "귀인로172번길", "42"])
                        if (fullAddress != null && !fullAddress.isEmpty()) {
                            String[] addressParts = fullAddress.split(" ");

                            // 3. 주소의 앞 2~3부분을 간결한 주소로, 나머지를 상세 주소로 설정합니다.
                            //    (예: "시/도 군/구" 까지를 간결한 주소로 취급)
                            if (addressParts.length > 2) {
                                // 앞 2개 파트(e.g., "경기도 안양시")를 간결한 주소로 합칩니다.
                                // 규칙은 비즈니스 로직에 맞게 조정 가능합니다. (e.g., 3개까지 합치기)
                                simpleAddress = addressParts[0] + " " + addressParts[1];

                                // 3번째 파트부터 끝까지를 상세 주소로 합칩니다.
                                detailAddress = String.join(" ", Arrays.copyOfRange(addressParts, 2, addressParts.length));
                            }
                        }

                        // ★★★★★★★★★★★ 주소 가공 로직 종료 ★★★★★★★★★★★

                        String cleanTitle = HTML_TAG_PATTERN.matcher(item.getTitle()).replaceAll("");

                        // 수정된 DTO 생성자에 맞게 값을 전달합니다.
                        return new StoreSearchResultDto(
                                idCounter.getAndIncrement(),
                                cleanTitle,
                                simpleAddress, // 가공된 간결한 주소
                                detailAddress  // 가공된 상세 주소
                        );
                    })
                    .collect(Collectors.toList());
        }

        return storeList;
    }
}
