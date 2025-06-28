package com.koo.bonscore.core.config.web.security.config;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class LoginSessionManager {

    // <사용자 ID, 발급된 Access Token>
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    // 무효화된 토큰 블랙리스트 (메모리 누수 방지를 위해 주기적으로 비워줘야 함)
    private final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 새로운 세션을 등록합니다. (로그인 성공 시)
     * @param userId 사용자 ID
     * @param token 발급된 Access Token
     */
    public void registerSession(String userId, String token) {
        // 기존 세션이 있다면 무효화 처리
        invalidateOldSession(userId);
        activeSessions.put(userId, token);
    }

    /**
     * 중복 로그인 여부를 확인합니다.
     * @param userId 사용자 ID
     * @return 중복 여부
     */
    public boolean isDuplicateLogin(String userId) {
        return activeSessions.containsKey(userId);
    }

    /**
     * 기존 세션을 무효화합니다. (기존 토큰을 블랙리스트에 추가)
     * @param userId 사용자 ID
     */
    public void invalidateOldSession(String userId) {
        if (activeSessions.containsKey(userId)) {
            String oldToken = activeSessions.remove(userId);
            if (oldToken != null) {
                tokenBlacklist.add(oldToken);
            }
        }
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인합니다.
     * @param token 확인할 Access Token
     * @return 블랙리스트 포함 여부
     */
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.contains(token);
    }

    /**
     * 주기적으로 만료된 블랙리스트 토큰을 정리하여 메모리 누수를 방지합니다.
     * (매일 새벽 4시에 실행)
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupBlacklist() {
        tokenBlacklist.removeIf(token -> {
            try {
                Claims claims = jwtTokenProvider.getClaimsFromToken(token); // 토큰에서 Claims 추출
                return claims.getExpiration().before(new Date()); // 만료 시간을 현재와 비교
            } catch (Exception e) {
                // 파싱에 실패한 토큰(이미 만료되었거나 형식이 다른 경우)은 제거 대상으로 간주
                return true;
            }
        });
    }

    /**
     * 사용자의 세션을 완전히 종료시킨다.
     * @param userId 사용자 ID
     * @param accessToken 무효화할 Access Token
     * @param refreshToken 무효화할 Refresh Token
     */
    public void logoutSession(String userId, String accessToken, String refreshToken) {
        // 1. 활성 세션 맵에서 사용자 제거
        activeSessions.remove(userId);

        // 2. 현재 사용하던 Access Token을 블랙리스트에 추가
        if (accessToken != null) {
            tokenBlacklist.add(accessToken);
        }

        // 3. Refresh Token도 블랙리스트에 추가하여 재발급 방지
        if (refreshToken != null) {
            tokenBlacklist.add(refreshToken);
        }
    }
}
