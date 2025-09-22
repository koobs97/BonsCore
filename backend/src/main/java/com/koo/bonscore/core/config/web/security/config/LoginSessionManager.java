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

//    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    // Redis를 사용하여 블랙리스트 관리
    private final RedisTemplate<String, String> redisTemplate; // RedisTemplate 주입
    private static final String ACTIVE_SESSION_KEY_PREFIX = "jwt:active:session:";
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:"; // Redis 키 접두사

    private final JwtTokenProvider jwtTokenProvider;

    public void registerSession(String userId, String token) {

        // 기존에 로그인된 세션이 있다면 무효화 처리 (Redis에서 삭제)
        invalidateOldSession(userId);

        // 새로운 세션을 Redis에 등록. Access Token의 만료 시간과 동일한 TTL 설정
        try {
            Claims claims = jwtTokenProvider.getClaimsFromToken(token);
            long expirationMillis = claims.getExpiration().getTime();
            long nowMillis = new Date().getTime();
            long remainingMillis = expirationMillis - nowMillis;

            if (remainingMillis > 0) {
                // key: "jwt:active:session:유저ID", value: "AccessToken 값"
                redisTemplate.opsForValue().set(
                        ACTIVE_SESSION_KEY_PREFIX + userId,
                        token,
                        Duration.ofMillis(remainingMillis) // 남은 만료 시간만큼 TTL 설정
                );
            }
        } catch (Exception e) {
            // 토큰 파싱 실패 시 로깅 또는 예외 처리
        }
    }

    public boolean isDuplicateLogin(String userId) {
        // Redis에 해당 유저의 활성 세션 키가 존재하는지 확인
        return Boolean.TRUE.equals(redisTemplate.hasKey(ACTIVE_SESSION_KEY_PREFIX + userId));
    }

    public void invalidateOldSession(String userId) {
        // Redis에서 기존 활성 세션 토큰을 가져온다.
        String oldToken = redisTemplate.opsForValue().get(ACTIVE_SESSION_KEY_PREFIX + userId);

        if (oldToken != null) {
            // 1. 기존 활성 세션 정보를 Redis에서 삭제한다.
            redisTemplate.delete(ACTIVE_SESSION_KEY_PREFIX + userId);
            // 2. 가져온 기존 토큰을 블랙리스트에 추가한다.
            addTokenToBlacklist(oldToken);
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

    // 로그아웃 메서드도 Redis를 사용하도록 수정
    public void logoutSession(String userId, String accessToken, String refreshToken) {
        // 1. 활성 세션 목록에서 제거
        redisTemplate.delete(ACTIVE_SESSION_KEY_PREFIX + userId);

        // 2. Access Token과 Refresh Token을 블랙리스트에 추가
        if (accessToken != null) {
            addTokenToBlacklist(accessToken);
        }
        if (refreshToken != null) {
            addTokenToBlacklist(refreshToken);
        }
    }
}
