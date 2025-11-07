package com.koo.bonscore.core.config.web.security.handler;

import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.auth.mapper.AuthMapper;
import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
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
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthMapper authMapper;

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

        // authentication.getName()은 CustomOAuth2UserService에서 반환한
        // DefaultOAuth2User의 nameAttributeKey에 해당하는 값을 반환합니다.
        // 하지만 우리는 DB 연동을 마친 '실제 사용자 ID'가 필요합니다.
        // CustomOAuth2UserService에서 반환된 user 객체에 접근해야 합니다.
        // 이를 위해 Principal을 OAuth2User로 캐스팅하고, attributes에서 userId를 다시 조합하는 대신,
        // 더 간단하고 확실한 방법은 authentication.getName()을 사용하는 것입니다.
        // Principal 객체 자체에서 사용자 이름을 가져올 수 있습니다.

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // attributes에서 직접 정보를 파싱하는 대신, CustomOAuth2UserService에서 연동/생성한
        // 실제 DB의 userId를 가져와야 합니다.
        // 가장 확실한 방법은, CustomOAuth2UserService에서 DB 조회를 마친 최종 userId를
        // Authentication 객체를 통해 전달받는 것입니다.
        // 현재 구조에서는 OAuth2User의 attributes에 있는 id로 다시 조합하고 있으므로,
        // CustomOAuth2UserService에서 처리한 실제 userId를 가져오는 로직이 필요합니다.

        // CustomOAuth2UserService의 saveOrUpdate에서 반환한 SignUpDto의 userId를 사용해야 합니다.
        // 현재는 그 정보가 여기까지 직접 전달되지 않으므로, DB를 다시 조회해야 합니다.

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String providerId = String.valueOf(attributes.get("id"));
        String provider = "kakao"; // 또는 다른 provider 로직

        // DB에서 provider 정보로 실제 userId를 다시 조회합니다. 이것이 가장 확실한 방법입니다.
        SignUpDto user = authMapper.findByProviderAndProviderId(provider, providerId);
        String userId = user.getUserId(); // "koobs97" 또는 "kakao_..."

        log.info("OAuth2 인증 성공. 최종 사용자 ID: {}", userId);

        // 이제 '실제' userId로 권한을 조회합니다.
        List<String> roles = authMapper.findRoleByUserId(userId);
        if (roles.isEmpty()) {
            // 이 경우는 로직상 발생하기 어렵지만, 방어코드로 추가
            roles.add("ROLE_USER");
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createToken(userId, roles, JwtTokenProvider.ACCESS_TOKEN_VALIDITY);
        String refreshToken = jwtTokenProvider.createToken(userId, List.of("ROLE_REFRESH"), JwtTokenProvider.REFRESH_TOKEN_VALIDITY);

        // ... (이하 Refresh Token 쿠키 설정 및 리디렉션 로직은 동일) ...
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(JwtTokenProvider.REFRESH_TOKEN_VALIDITY / 1000)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("token", accessToken)
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }
}
