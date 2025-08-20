package com.koo.bonscore.common.api.naver;

import com.koo.bonscore.common.api.naver.config.NaverProperties;
import com.koo.bonscore.common.api.naver.dto.NaverApiResponseDto;
import com.koo.bonscore.common.api.naver.dto.blog.NaverBlogSearchResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component // 다른 곳에서 주입받아 사용할 수 있도록 Bean으로 등록
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 만들어줌
public class NaverApiClient {

    private final RestTemplate restTemplate;
    private final NaverProperties naverProperties;

    public NaverApiResponseDto searchLocal(String query) {
        log.info("Naver API - 지역 검색 호출: query={}", query);

        URI uri = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com")
                .path("/v1/search/local.json")
                .queryParam("query", query)
                .queryParam("display", 5)
                .queryParam("start", 1)
                .queryParam("sort", "random")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        RequestEntity<Void> req = RequestEntity
                .get(uri)
                .header("X-Naver-Client-Id", naverProperties.getId())
                .header("X-Naver-Client-Secret", naverProperties.getSecret())
                .build();

        ResponseEntity<NaverApiResponseDto> result = restTemplate.exchange(req, NaverApiResponseDto.class);

        if (!result.getStatusCode().is2xxSuccessful()) {
            log.error("Naver API 호출 실패. Status: {}, Body: {}", result.getStatusCode(), result.getBody());
            // 예외 처리 또는 null 대신 빈 객체 반환 등의 처리 필요
            return new NaverApiResponseDto(); // 혹은 커스텀 예외 발생
        }

        log.info("Naver API 응답: {}", result.getBody());
        return result.getBody();
    }

    public NaverBlogSearchResponseDto searchBlog(String query) {
        log.info("Naver API - 블로그 검색 호출: query={}", query);

        URI uri = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com")
                .path("/v1/search/blog.json") // <-- 변경점 1: path 변경
                .queryParam("query", query)
                .queryParam("display", 1) // 블로그 개수만 필요하면 display=1로 충분
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        RequestEntity<Void> req = RequestEntity
                .get(uri)
                .header("X-Naver-Client-Id", naverProperties.getId())
                .header("X-Naver-Client-Secret", naverProperties.getSecret())
                .build();

        // <-- 변경점 2: 반환 DTO 클래스 변경
        ResponseEntity<NaverBlogSearchResponseDto> result = restTemplate.exchange(req, NaverBlogSearchResponseDto.class);

        if (!result.getStatusCode().is2xxSuccessful()) {
            log.error("Naver API 블로그 검색 호출 실패. Status: {}, Body: {}", result.getStatusCode(), result.getBody());
            return new NaverBlogSearchResponseDto(); // 실패 시 빈 객체 반환
        }

        log.info("Naver API 블로그 검색 응답: {}", result.getBody());
        return result.getBody();
    }
}
