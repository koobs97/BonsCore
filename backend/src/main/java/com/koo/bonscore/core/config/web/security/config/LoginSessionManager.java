package com.koo.bonscore.core.config.web.security.config;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
        redisTemplate.expire(ACTIVE_TOKENS_KEY_PREFIX + userId, Duration.ofDays(7));
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
}
