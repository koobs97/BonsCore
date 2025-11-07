package com.koo.bonscore.biz.auth.service;

import com.koo.bonscore.biz.auth.dto.OAuthAttributes;
import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.auth.mapper.AuthMapper;
import com.koo.bonscore.core.config.enc.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * CustomOAuth2UserService.java
 * 설명 : Spring Security의 OAuth2 로그인 성공 후 사용자 정보를 처리하는 커스텀 서비스 클래스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final AuthMapper authMapper;
    private final EncryptionService encryptionService;

    /**
     * OAuth2 공급자(Provider)로부터 사용자 정보를 로드하여 처리하는 메소드
     *
     * @param userRequest OAuth2 공급자의 액세스 토큰과 클라이언트 등록 정보가 포함된 요청 객체
     * @return 인증된 사용자 정보와 권한을 담은 {@link DefaultOAuth2User} 객체
     * @throws OAuth2AuthenticationException OAuth2AuthenticationException 사용자 정보를 로드하는 과정에서 발생하는 모든 OAuth2 관련 예외
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2UserService를 사용하여 공급자로부터 사용자 정보를 가져온다.
        OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = oAuth2UserService.loadUser(userRequest);

        // 2. 요청 정보를 통해 현재 로그인 중인 소셜 서비스(registrationId)와 주요 속성 키(userNameAttributeName)를 확인
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 3. 각 소셜 서비스의 상이한 속성 구조를 표준화된 OAuthAttributes 객체로 변환
        OAuthAttributes oAuthAttributes = OAuthAttributes.of(registrationId, userNameAttributeName, attributes);
        assert oAuthAttributes != null;
        log.info("oAuthAttributes: {}", oAuthAttributes.getAttributes());

        // 4. 데이터베이스에 사용자를 저장하거나 업데이트
        SignUpDto user = saveOrUpdate(oAuthAttributes);

        // 5. 사용자의 권한 정보를 조회
        List<String> roles = authMapper.findRoleByUserId(user.getUserId());
        if (roles.isEmpty()) {
            roles.add("ROLE_USER");
        }

        // 6. Spring Security의 Principal 객체인 DefaultOAuth2User를 생성하여 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(String.join(",", roles))),
                oAuthAttributes.getAttributes(),
                oAuthAttributes.getNameAttributeKey()
        );
    }

    /**
     * 소셜 로그인 사용자를 저장하거나, 기존 사용자와 연동하는 메소드
     * 동시성 문제를 방지하기 위해 synchronized 처리.
     *
     * @param attributes 소셜 플랫폼별 사용자 정보를 표준화한 DTO
     * @return 데이터베이스에 저장되거나 업데이트된 사용자 정보를 담은 {@link SignUpDto} 객체
     */
    private synchronized SignUpDto saveOrUpdate(OAuthAttributes attributes) {
        // 1. Provider와 Provider ID로 이미 연동된 사용자인지 확인
        SignUpDto user = authMapper.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId());
        if (user != null) {
            log.info("기존 소셜 연동 계정을 찾았습니다: {}", user.getUserId());
            return user;
        }

        // 2. 카카오에서 이메일 정보를 제공했는지 확인
        if (attributes.getEmail() == null) {
            log.warn("카카오로부터 이메일 정보를 받지 못해 신규 가입으로 처리합니다. ProviderId: {}", attributes.getProviderId());
            return createNewSocialUser(attributes);
        }

        // 3. 이메일 해시로 기존 계정이 있는지 확인하여 연동 시도
        String emailHash = encryptionService.hashWithSalt(attributes.getEmail());
        SignUpDto existingUser = authMapper.findByEmailHash(emailHash);

        if (existingUser != null) {
            // 3-1. 이메일이 일치하는 기존 계정이 있으면, 해당 계정에 소셜 정보 업데이트 (계정 연동)
            log.info("이메일이 일치하는 기존 계정({})을 찾아 소셜 계정과 연동합니다.", existingUser.getUserId());
            existingUser.setOauthProvider(attributes.getProvider());
            existingUser.setOauthProviderId(attributes.getProviderId());
            authMapper.updateSocialInfo(existingUser);
            return existingUser;
        } else {
            // 4. 연동할 기존 계정이 없으면, 신규 소셜 계정으로 회원가입
            log.info("연동할 기존 계정이 없어 신규 소셜 계정으로 가입합니다. Email: {}", attributes.getEmail());
            return createNewSocialUser(attributes);
        }
    }

    /**
     * 새로운 소셜 계정 사용자를 생성하고 DB에 저장하는 메소드
     *
     * @param attributes 신규 가입에 필요한 사용자 정보를 담은 {@link OAuthAttributes} 객체
     * @return 데이터베이스에 성공적으로 저장된 신규 사용자 정보를 담은 {@link SignUpDto} 객체
     */
    private SignUpDto createNewSocialUser(OAuthAttributes attributes) {
        SignUpDto newUser = attributes.toSignUpDto();

        SignUpDto encryptedUser = SignUpDto.builder()
                .userId(newUser.getUserId())
                .userName(newUser.getUserName() != null ? encryptionService.encrypt(newUser.getUserName()) : null)
                .email(newUser.getEmail() != null ? encryptionService.encrypt(newUser.getEmail()) : null)
                .emailHash(newUser.getEmail() != null ? encryptionService.hashWithSalt(newUser.getEmail()) : null)
                .phoneNumber(newUser.getPhoneNumber() != null ? encryptionService.encrypt(newUser.getPhoneNumber()) : null)
                .birthDate(newUser.getBirthDate() != null ? encryptionService.encrypt(newUser.getBirthDate()) : null)
                .genderCode(newUser.getGenderCode())
                .password(newUser.getPassword())
                .oauthProvider(newUser.getOauthProvider())
                .oauthProviderId(newUser.getOauthProviderId())
                .termsAgree1(newUser.getTermsAgree1())
                .termsAgree2(newUser.getTermsAgree2())
                .termsAgree3(newUser.getTermsAgree3())
                .termsAgree4(newUser.getTermsAgree4())
                .createdAt(newUser.getCreatedAt())
                .updatedAt(newUser.getUpdatedAt())
                .build();

        authMapper.signUpUser(encryptedUser);
        authMapper.signUpUserRole(encryptedUser);
        return encryptedUser;
    }
}