package com.koo.bonscore.biz.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * <pre>
 * RecaptchaService.java
 * 설명 : reCAPTCHA 검증 서비스 클래스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecaptchaService {

    @Value("${api.recaptcha.secret}")
    private String recaptchaSecret;

    @Value("${api.recaptcha.verify-url}")
    private String recaptchaVerifyUrl;

    private final WebClient webClient = WebClient.create();

    /**
     * reCAPTCHA 토큰의 유효성을 검증한다.
     *
     * @param recaptchaToken 프론트엔드에서 전달받은 reCAPTCHA 응답 토큰
     * @return 검증 성공 시 true, 실패 시 false를 포함하는 Mono<Boolean>
     */
    public Mono<Boolean> verifyRecaptcha(String recaptchaToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secret", recaptchaSecret);
        formData.add("response", recaptchaToken);

        return webClient.post()
                .uri(recaptchaVerifyUrl)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    boolean success = (Boolean) response.getOrDefault("success", false);
                    if (!success) {
                        log.warn("reCAPTCHA verification failed. Response: {}", response);
                    }
                    return success;
                })
                .doOnError(e -> log.error("Error during reCAPTCHA verification request.", e))
                .onErrorReturn(false); // 에러 발생 시 검증 실패로 처리
    }
}