package com.koo.bonscore.biz.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * <pre>
 * LoginAttemptService.java
 * 설명 : 로그인 실패 횟수를 관리하는 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-10-27
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int CAPTCHA_THRESHOLD = 3;
    private static final int MAX_ATTEMPT = 5;       // 최대 실패 횟수
    private static final long LOCK_TIME_MIN = 5;   // 잠금 시간 (분)
    
    private final StringRedisTemplate redisTemplate;

    /**
     * 로그인 성공 시 호출
     * @param userId 사용자 ID
     */
    public void loginSucceeded(String userId) {
        String key = "login:attempt:" + userId;
        redisTemplate.delete(key);
    }

    /**
     * 로그인 실패 시 호출
     * @param userId 사용자 ID
     */
    public void loginFailed(String userId) {
        String key = "login:attempt:" + userId;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.increment(key);

        // 실패 기록은 잠금 시간만큼만 유지
        redisTemplate.expire(key, LOCK_TIME_MIN, TimeUnit.MINUTES);
    }

    private String getKey(String userId) {
        return "login:attempt:" + userId;
    }

    /**
     * 계정이 잠겼는지 확인
     * @param userId 사용자 ID
     * @return 계정잠김여부
     */
    public boolean isCaptchaRequired(String userId) {
        String attemptsStr = redisTemplate.opsForValue().get(getKey(userId));
        if (attemptsStr == null) return false;

        int attempts = Integer.parseInt(attemptsStr);
        return attempts >= CAPTCHA_THRESHOLD;
    }

    /**
     * 계정이 잠겼는지 확인 (5번 이상 실패 시)
     * @param userId 사용자 ID
     * @return 계정 잠김 여부
     */
    public boolean isAccountLocked(String userId) {
        String attemptsStr = redisTemplate.opsForValue().get(getKey(userId));
        if (attemptsStr == null) return false;

        int attempts = Integer.parseInt(attemptsStr);
        return attempts >= MAX_ATTEMPT;
    }

}
