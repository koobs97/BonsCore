package com.koo.bonscore.core.config.web.security.config;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class LoginSessionManager {

    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    // Redis를 사용하여 블랙리스트 관리
    private final RedisTemplate<String, String> redisTemplate; // RedisTemplate 주입
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:"; // Redis 키 접두사

    private final JwtTokenProvider jwtTokenProvider;

    public void registerSession(String userId, String token) {
        invalidateOldSession(userId);
        activeSessions.put(userId, token);
    }

    public boolean isDuplicateLogin(String userId) {
        return activeSessions.containsKey(userId);
    }

    public void invalidateOldSession(String userId) {
        if (activeSessions.containsKey(userId)) {
            String oldToken = activeSessions.remove(userId);
            if (oldToken != null) {
                addTokenToBlacklist(oldToken); // Redis에 추가
            }
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
     * 토큰이 블랙리스트(Redis)에 있는지 확인합니다.
     * @param token 확인할 Access Token
     * @return 블랙리스트 포함 여부
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token));
    }

    // Redis 사용 시에는 이 스케줄링 메서드가 필요 없어집니다.
    // @Scheduled(cron = "0 0 4 * * *")
    // public void cleanupBlacklist() { ... }

    public void logoutSession(String userId, String accessToken, String refreshToken) {
        activeSessions.remove(userId);

        if (accessToken != null) {
            addTokenToBlacklist(accessToken);
        }
        if (refreshToken != null) {
            addTokenToBlacklist(refreshToken); // Refresh Token도 블랙리스트에 추가
        }
    }
}
