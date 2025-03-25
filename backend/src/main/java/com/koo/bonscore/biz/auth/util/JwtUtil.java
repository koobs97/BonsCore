package com.koo.bonscore.biz.auth.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private static final String SECRET_KEY = "Y/i07yxYfyhiGpquS5JuKCXuS7TbH1PD5+OzdMGcx4I="; // 256비트 키 필요
    private static final long EXPIRATION_TIME = 1000 * 60 * 60; // 1시간

    /**
     * JWT 생성 메서드
     * @param username
     * @return
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)       // 유저정보
                .setIssuedAt(new Date())    // 발급시간
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 만료시간
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256) // 서명
                .compact();
    }

    /**
     * JWT 검증 메서드
     * @param token
     * @return
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * JWT에서 유저정보 추출
     * @param token
     * @return
     */
    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
