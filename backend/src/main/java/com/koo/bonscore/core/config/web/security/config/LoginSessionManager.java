package com.koo.bonscore.core.config.web.security.config;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSessionManager {

    // Redis를 사용하여 블랙리스트 관리
    private final RedisTemplate<String, String> redisTemplate;

    private static final String ACTIVE_TOKENS_KEY_PREFIX = "jwt:active:tokens:";
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 새로운 세션(토큰)을 등록
     * 발급된 토큰을 해당 사용자의 활성 토큰 목록(Set)에 추가한다.
     *
     * @param userId 사용자 ID
     * @param token 새로 발급된 Access Token
     */
    public void registerSession(String userId, String token) {
        // 새로운 토큰을 사용자의 활성 토큰 Set에 추가합니다.
        redisTemplate.opsForSet().add(ACTIVE_TOKENS_KEY_PREFIX + userId, token);
        // Set 자체에 대한 만료 시간을 넉넉하게 설정 (예: 7일)
        // 개별 토큰의 만료는 JWT 자체의 만료 시간으로 검증됩니다.
        // redisTemplate.expire(ACTIVE_TOKENS_KEY_PREFIX + userId, Duration.ofDays(7));
    }

    /**
     * 중복 로그인이 있는지 확인한다. (활성 세션이 하나라도 있는지)
     *
     * @param userId 사용자 ID
     * @return 활성 세션 존재 여부
     */
    public boolean isDuplicateLogin(String userId) {
        // 활성 토큰 Set에 멤버가 1개 이상 있는지 확인
        Long size = redisTemplate.opsForSet().size(ACTIVE_TOKENS_KEY_PREFIX + userId);
        return size != null && size > 0;
    }

    /**
     * 특정 사용자의 모든 기존 세션을 무효화한다.
     * 새로운 강제 로그인 시 호출되어, 해당 사용자의 모든 다른 기기 로그인을 끊는다.
     *
     * @param userId 사용자 ID
     */
    public void invalidateAllOldSessions(String userId) {
        Set<String> activeTokens = redisTemplate.opsForSet().members(ACTIVE_TOKENS_KEY_PREFIX + userId);

        if (activeTokens != null && !activeTokens.isEmpty()) {
            // 1. 가져온 모든 기존 활성 토큰을 블랙리스트에 추가합니다.
            for (String token : activeTokens) {
                addTokenToBlacklist(token);
            }
            // 2. 기존 활성 세션 정보 (Set 자체)를 Redis에서 삭제합니다.
            redisTemplate.delete(ACTIVE_TOKENS_KEY_PREFIX + userId);
        }
    }

    /**
     * 토큰을 블랙리스트(Redis)에 추가하고 만료 시간을 설정합니다.
     * @param token 블랙리스트에 추가할 토큰
     */
    private void addTokenToBlacklist(String token) {
        try {
            Claims claims = jwtTokenProvider.getClaimsFromToken(token);
            long expirationMillis = claims.getExpiration().getTime();
            long nowMillis = new Date().getTime();

            // 남은 유효 시간 계산. 만료된 토큰은 바로 제거되도록 작은 값 설정 또는 0
            long remainingMillis = expirationMillis - nowMillis;
            if (remainingMillis > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "invalidated", Duration.ofMillis(remainingMillis));
            } else {
                // 이미 만료된 토큰이라면 블랙리스트에 추가할 필요 없음 (혹은 아주 짧은 TTL로 즉시 제거)
                // redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "invalidated", Duration.ofSeconds(1));
            }
        } catch (Exception e) {
            // 토큰 파싱 실패 시 (유효하지 않거나 이미 만료된 토큰)
            // 블랙리스트에 추가하지 않거나, 아주 짧은 TTL로 추가
            redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "invalidated", Duration.ofSeconds(10)); // 짧은 시간 후 자동 제거
        }
    }

    /**
     * 토큰이 블랙리스트(Redis)에 있는지 확인한다.
     * @param token 확인할 Access Token
     * @return 블랙리스트 포함 여부
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token));
    }

    /**
     * 로그아웃 시 특정 세션을 무효화한다.
     * @param userId 사용자 ID
     * @param accessToken 무효화할 Access Token
     * @param refreshToken 무효화할 Refresh Token
     */
    public void logoutSession(String userId, String accessToken, String refreshToken) {
        // 1. 활성 세션 목록(Set)에서 해당 Access Token 제거
        if (accessToken != null) {
            redisTemplate.opsForSet().remove(ACTIVE_TOKENS_KEY_PREFIX + userId, accessToken);
            addTokenToBlacklist(accessToken);
        }
        // 2. Refresh Token도 블랙리스트에 추가
        if (refreshToken != null) {
            addTokenToBlacklist(refreshToken);
        }
    }

    /**
     * 주기적으로 만료된 JWT 토큰을 활성 세션 목록에서 정리합니다.
     * 매 시간 정각에 실행됩니다. (cron = "0 0 * * * *")
     */
    @Scheduled(cron = "0 0 * * * *") // 매 시간 실행 (테스트 시에는 @Scheduled(fixedRate = 60000) // 1분마다 실행 등으로 변경)
    public void cleanupExpiredTokens() {
        log.info("만료된 활성 세션 정리를 시작합니다...");
        try {
            // "jwt:active:tokens:*" 패턴을 가진 모든 키를 조회합니다.
            Set<String> keys = redisTemplate.keys(ACTIVE_TOKENS_KEY_PREFIX + "*");
            if (keys.isEmpty()) {
                log.info("정리할 활성 세션이 없습니다.");
                return;
            }

            for (String key : keys) {
                // 각 사용자의 활성 토큰 Set을 가져옵니다.
                Set<String> tokens = redisTemplate.opsForSet().members(key);
                if (tokens != null) {
                    for (String token : tokens) {
                        // 각 토큰이 만료되었는지 확인합니다.
                        if (jwtTokenProvider.isTokenExpired(token)) {
                            // 만료되었다면 Set에서 제거합니다.
                            redisTemplate.opsForSet().remove(key, token);
                            log.debug("만료된 토큰 제거: {}", token);
                        }
                    }
                }

                // 만약 Set에 토큰이 하나도 남지 않았다면 Set 키 자체를 삭제하여 메모리를 절약합니다.
                if (Objects.requireNonNull(redisTemplate.opsForSet().size(key)) == 0) {
                    redisTemplate.delete(key);
                    log.debug("빈 활성 세션 Set 제거: {}", key);
                }
            }
        } catch (Exception e) {
            log.error("만료된 토큰 정리 중 오류 발생", e);
        }
        log.info("만료된 활성 세션 정리를 완료했습니다.");
    }
}
