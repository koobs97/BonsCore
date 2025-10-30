package com.koo.bonscore.biz.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * <pre>
 * PwnedPasswordService.java
 * 설명 : 비밀번호가 유출되었는지 확인하는 api 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-10-30
 */
@Slf4j
@Service
public class PwnedPasswordService {

    private final WebClient webClient;

    // WebClient를 사용하여 HIBP API와 비동기 통신을 설정.
    public PwnedPasswordService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.pwnedpasswords.com").build();
    }

    /**
     * 비밀번호가 유출되었는지 확인하는 메인 메서드
     *
     * @param password 확인할 원본 비밀번호
     * @return 유출되었다면 true, 아니면 false를 반환하는 Mono<Boolean>
     */
    public Mono<Boolean> isPasswordPwned(String password) {
        try {
            // 1. 비밀번호를 SHA-1로 해싱.
            String sha1Hash = toSHA1(password);

            // 2. 해시의 앞 5자리(prefix)와 나머지(suffix)를 분리.
            String prefix = sha1Hash.substring(0, 5);
            String suffix = sha1Hash.substring(5);

            // 3. k-Anonymity API를 호출.
            return webClient.get()
                    .uri("/range/" + prefix)
                    .retrieve()
                    .bodyToMono(String.class)
                    // API 응답에서 suffix를 찾아 유출 여부를 확인.
                    .map(responseBody -> responseBody.lines()
                            .anyMatch(line -> line.startsWith(suffix)))
                    .doOnError(error -> log.error("Error checking pwned password: {}", error.getMessage()))
                    // 에러 발생 시 안전하게 '유출되지 않음'으로 처리.
                    .onErrorReturn(false);

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-1 algorithm not found", e);
            // 해싱 알고리즘을 찾을 수 없는 심각한 문제 발생 시, 안전하게 false를 반환.
            return Mono.just(false);
        }
    }

    /**
     * 문자열을 SHA-1 해시 값으로 변환
     *
     * @param input 변환할 문자열
     * @return 대문자 SHA-1 해시 문자열
     */
    private String toSHA1(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash).toUpperCase();
    }

}
