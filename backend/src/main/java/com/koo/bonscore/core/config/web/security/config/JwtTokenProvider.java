package com.koo.bonscore.core.config.web.security.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * <pre>
 * JwtTokenProvider.java
 * 설명 : JWT(JSON Web Token)의 생성, 검증, 파싱 등 전반적인 처리를 담당하는 유틸리티 클래스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-01
 */
@Component
public class JwtTokenProvider {

    /**
     * JWT 서명 및 검증에 사용될 암호화 키
     * 생성자에서 BASE64로 인코딩된 시크릿 키로부터 초기화된다.
     */
    private final Key key;

    /**
     * Access Token의 기본 유효 기간 (15분)
     */
    public static final long ACCESS_TOKEN_VALIDITY = 15 * 60 * 1000; // 15분
    /**
     * Refresh Token의 기본 유효 기간 (7일)
     */
    public static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000; //

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    /**
     * `application.yml`에 정의된 JWT 시크릿 키를 주입받아 서명 키를 초기화하는 생성자
     *
     * @param secretKey BASE64로 인코딩된 JWT 시크릿 키 문자열
     */
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 사용자 ID와 권한 정보를 바탕으로 새로운 JWT를 생성
     *
     * @param userId           토큰의 주체(subject)가 될 사용자의 고유 ID
     * @param roles            사용자의 권한 목록. "roles"라는 이름의 클레임에 저장됩니다.
     * @param expirationMillis 토큰의 유효 기간 (밀리초 단위)
     * @return 생성된 JWT 문자열
     */
    public String createToken(String userId, List<String> roles, long expirationMillis) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("roles", roles);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * HTTP 요청의 "Authorization" 헤더에서 "Bearer " 접두사를 제거하고 순수한 JWT를 추출
     *
     * @param request HTTP 서블릿 요청 객체
     * @return 추출된 JWT 문자열, 또는 토큰이 없거나 형식이 올바르지 않을 경우 null
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER);
        if (bearerToken != null && bearerToken.startsWith(PREFIX)) {
            return bearerToken.substring(PREFIX.length());
        }
        return null;
    }

    /**
     * 주어진 토큰의 서명과 유효 기간을 검증
     *
     * @param token 검증할 JWT 문자열
     * @return 토큰이 유효하면 true, 그렇지 않으면(만료, 서명 불일치, 형식 오류 등) false
     */
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return !claims.getBody().getExpiration().before(new Date()); // 만료 여부 검사
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰에서 사용자 ID(Subject)를 추출
     *
     * @param token 사용자 ID를 추출할 JWT 문자열
     * @return 추출된 사용자 ID
     * @throws ExpiredJwtException 토큰이 만료된 경우
     * @throws JwtException        토큰의 형식이 잘못되었거나 서명이 유효하지 않은 경우
     */
    public String getUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * 유효한 토큰에서 클레임(Payload) 정보를 추출
     *
     * @param token 클레임을 추출할 JWT 문자열
     * @return 토큰에 포함된 {@link Claims} 객체
     * @throws ExpiredJwtException 토큰이 만료된 경우
     * @throws JwtException        토큰의 형식이 잘못되었거나 서명이 유효하지 않은 경우
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key) // 토큰 생성 시 사용한 secretKey
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰에서 클레임 정보를 추출
     *
     * @param token 클레임을 추출할 JWT 문자열
     * @return 토큰에 포함된 {@link Claims} 객체
     * @see #parseClaims(String)
     */
    public Claims getClaimsFromToken(String token) {
        return parseClaims(token);
    }

    /**
     * 토큰을 파싱하여 클레임 정보를 반환하되, {@link ExpiredJwtException}이 발생했을 때
     * 예외적으로 만료된 토큰의 클레임이라도 반환하도록 처리하는 메소드
     *
     * @param accessToken 파싱할 JWT 문자열
     * @return 토큰에 포함된 {@link Claims} 객체. 토큰이 만료되어도 클레임은 반환
     */
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이라도 Claim 값은 필요할 수 있으므로, 예외적으로 Claim을 반환한다.
            return e.getClaims();
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            final Date expiration = getClaimsFromToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            // 파싱 자체가 실패하면 (예: 형식이 다른 토큰, 이미 만료되어 파싱 에러 발생) 만료된 것으로 간주
            return true;
        }
    }
}
