package com.koo.bonscore.core.config.web.security.config;

import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private final long expirationMs = 1000L * 60 * 60 * 2; // 2시간

    public static final long ACCESS_TOKEN_VALIDITY = 15 * 60 * 1000; // 15분
    public static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000; //

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    // 토큰 생성
    public String createToken(String userId, long expirationMillis) {
        Claims claims = Jwts.claims().setSubject(userId);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        Key key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 요청에서 토큰 추출
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER);
        if (bearerToken != null && bearerToken.startsWith(PREFIX)) {
            return bearerToken.substring(PREFIX.length());
        }
        return null;
    }

    // 토큰이 유효한지
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token);

            return !claims.getBody().getExpiration().before(new Date()); // 만료 여부 검사
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        String userId = getUserId(token); // subject 에 저장된 userId
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER")); // 혹은 토큰에서 roles 파싱

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(userId, "", authorities);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Claims getClaimsFromToken(String token) {
        return parseClaims(token);
    }

    private Claims parseClaims(String accessToken) {
        try {
            Key key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이라도 Claim 값은 필요할 수 있으므로, 예외적으로 Claim을 반환합니다.
            // LoginSessionManager의 cleanupBlacklist에서 만료 여부를 확인하기 위함입니다.
            return e.getClaims();
        }
    }
}
