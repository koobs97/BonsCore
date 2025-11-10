package com.koo.bonscore.core.config.web.security.handler;

import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.auth.mapper.AuthMapper;
import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.config.web.security.config.LoginSessionManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * OAuth2LoginSuccessHandler.java
 * 설명 : OAuth2 로그인 성공 후의 로직을 처리하는 커스텀 핸들러
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthMapper authMapper;
    private final LoginSessionManager loginSessionManager;

    /**
     * 프론트엔드의 OAuth2 리디렉션 처리 페이지 URL
     */
    @Value("${oauth2.redirect.url}")
    private String redirectUrl;

    /**
     * OAuth2 인증이 성공적으로 완료되었을 때 실행되는 메소드
     * <p>
     * 1. 인증된 {@link Authentication} 객체에서 사용자 정보를 추출합니다.<br>
     * 2. 데이터베이스를 조회하여 최종적으로 확정된 사용자 ID와 권한을 가져옵니다.<br>
     * 3. JWT Access Token과 Refresh Token을 생성합니다.<br>
     * 4. Refresh Token을 HttpOnly, Secure 속성의 쿠키에 담아 응답 헤더에 추가합니다.<br>
     * 5. Access Token을 쿼리 파라미터에 담아 프론트엔드의 리디렉션 URL로 사용자를 이동시킵니다.
     *
     * @param request        HTTP 요청 객체
     * @param response       HTTP 응답 객체
     * @param authentication Spring Security가 생성한 최종 인증 객체. {@link OAuth2User}를 Principal로 포함합니다.
     * @throws IOException      리디렉션 과정에서 입출력 예외 발생 시
     * @throws ServletException 서블릿 관련 예외 발생 시
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 Login 성공!");

        // 1. Authentication 객체를 OAuth2AuthenticationToken으로 캐스팅하여 Provider(registrationId)를 가져옵니다.
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        String provider = authToken.getAuthorizedClientRegistrationId(); // "kakao", "naver" 등

        // 2. Principal(OAuth2User)에서 속성(attributes)을 가져옵니다.
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 3. Provider에 따라 고유 ID(providerId)를 추출합니다.
        //    (네이버는 'response' 객체 안에 id가 있고, 카카오는 최상위에 id가 있습니다.)
        String providerId;
        if ("naver".equals(provider)) {
            providerId = String.valueOf(attributes.get("id"));
        } else if ("google".equals(provider)) {
            providerId = (String) attributes.get("sub");
        }
        else {
            providerId = String.valueOf(attributes.get("id"));
        }

        // 4. Provider와 ProviderId를 사용해 DB에서 최종 사용자 정보를 조회합니다.
        SignUpDto user = authMapper.findByProviderAndProviderId(provider, providerId);

        // 5. 사용자 정보가 없는 경우에 대한 예외 처리 (이론상 발생하면 안 됨)
        if (user == null) {
            log.error("OAuth2 인증 후 사용자 정보를 DB에서 찾을 수 없습니다. Provider: {}, ProviderId: {}", provider, providerId);
            // 에러 페이지나 로그인 페이지로 리다이렉트
            getRedirectStrategy().sendRedirect(request, response, "/login?error=OAuth2UserNotFound");
            return;
        }

        String userId = user.getUserId();
        log.info("OAuth2 인증 성공. 최종 사용자 ID: {}", userId);

        // 신규 구글 유저인지 판별
        boolean isNewUser = "google".equals(provider) && (user.getBirthDate() == null || user.getGenderCode() == null);
        log.info("신규 유저 여부: {}", isNewUser);

        // 6. 실제 userId로 권한을 조회합니다.
        List<String> roles = authMapper.findRoleByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            roles = List.of("ROLE_USER"); // 기본 권한 부여
        }

        // 7. JWT 토큰 생성
        String accessToken = jwtTokenProvider.createToken(userId, roles, JwtTokenProvider.ACCESS_TOKEN_VALIDITY);
        String refreshToken = jwtTokenProvider.createToken(userId, List.of("ROLE_REFRESH"), JwtTokenProvider.REFRESH_TOKEN_VALIDITY);

        // 8. 세션 매니저에 새로운 세션 등록
        loginSessionManager.registerSession(userId, accessToken);

        // 9. Refresh Token을 쿠키에 담아 응답
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)  // HTTPS 환경에서만 전송
                .sameSite("Strict") // CSRF 공격 방어
                .path("/")
                .maxAge(JwtTokenProvider.REFRESH_TOKEN_VALIDITY / 1000)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 10. Access Token을 쿼리 파라미터에 담아 프론트엔드로 리디렉션
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("token", accessToken) // 프론트엔드와 약속된 파라미터명
                .queryParam("oauthProvider", provider)
                .queryParam("isNewUser", isNewUser)
                .build().toUriString();

        // 부모 클래스의 리다이렉션 전략 사용
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
