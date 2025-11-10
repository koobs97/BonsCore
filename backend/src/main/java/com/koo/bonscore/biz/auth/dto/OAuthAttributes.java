package com.koo.bonscore.biz.auth.dto;

import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * <pre>
 * OAuthAttributes.java
 * 설명 : OAuth2 인증을 통해 얻은 사용자 정보를 통합적으로 관리하는 DTO 클래스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-07
 */
@Getter
public class OAuthAttributes {
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final String email;
    private final String provider;
    private final String providerId;

    // 추가 정보 필드
    private final String name;         // 이름
    private final String gender;       // 성별 (male/female)
    private final String birthyear;    // 출생연도
    private final String birthday;     // 생일 (MMDD 형식)
    private final String phoneNumber;  // 전화번호

    /**
     * 빌더 패턴을 사용하여 {@code OAuthAttributes} 객체를 생성하는 생성자
     *
     * @param attributes        원본 사용자 속성 맵
     * @param nameAttributeKey  사용자 이름 속성 키
     * @param email             이메일
     * @param provider          소셜 플랫폼 이름
     * @param providerId        소셜 플랫폼 고유 ID
     * @param name              이름
     * @param gender            성별
     * @param birthyear         출생 연도
     * @param birthday          생일
     * @param phoneNumber       휴대전화 번호
     */
    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String email, String provider, String providerId, String name, String gender, String birthyear, String birthday, String phoneNumber) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.name = name;
        this.gender = gender;
        this.birthyear = birthyear;
        this.birthday = birthday;
        this.phoneNumber = phoneNumber;
    }

    /**
     * 소셜 플랫폼의 종류(registrationId)에 따라 적절한 파싱 메소드를 호출하는 정적 팩토리 메소드
     *
     * @param registrationId        로그인한 소셜 플랫폼의 등록 ID (예: "kakao")
     * @param userNameAttributeName 사용자 이름 속성 키
     * @param attributes            OAuth2 공급자로부터 받은 원본 사용자 속성 맵
     * @return 각 플랫폼에 맞게 파싱되고 표준화된 {@code OAuthAttributes} 객체
     */
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        }
        if ("naver".equals(registrationId)) {
            return ofNaver(userNameAttributeName, attributes);
        }
        return null;
    }

    /**
     * 카카오의 사용자 정보 응답(attributes)을 파싱하여 {@code OAuthAttributes} 객체를 생성한다.
     * 카카오 응답은 'kakao_account'라는 중첩된 맵 안에 대부분의 개인정보가 포함되어 있다.
     *
     * @param userNameAttributeName 카카오의 경우 "id"가 된다.
     * @param attributes            카카오로부터 받은 원본 사용자 속성 맵
     * @return 카카오 정보로 채워진 {@code OAuthAttributes} 객체
     */
    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        // 카카오 응답은 'kakao_account' 안에 모든 정보가 들어있습니다.
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        String rawPhoneNumber = (String) kakaoAccount.get("phone_number");
        String formattedPhoneNumber = normalizePhoneNumber(rawPhoneNumber);

        return OAuthAttributes.builder()
                .email((String) kakaoAccount.get("email"))
                .provider("kakao")
                .providerId(String.valueOf(attributes.get("id")))
                // kakao_account에서 추가 정보 파싱
                .name((String) kakaoAccount.get("name"))
                .gender((String) kakaoAccount.get("gender"))
                .birthyear((String) kakaoAccount.get("birthyear"))
                .birthday((String) kakaoAccount.get("birthday"))
                .phoneNumber(formattedPhoneNumber)
                .nameAttributeKey(userNameAttributeName)
                .attributes(attributes)
                .build();
    }

    /**
     * 네이버의 사용자 정보 응답(attributes)을 파싱하여 {@code OAuthAttributes} 객체를 생성한다.
     * 네이버 응답은 'response'라는 중첩된 맵 안에 대부분의 개인정보가 포함되어 있다.
     *
     * @param userNameAttributeName 네이버의 경우 "response"가 된다.
     * @param attributes            네이버로부터 받은 원본 사용자 속성 맵
     * @return 네이버 정보로 채워진 {@code OAuthAttributes} 객체
     */
    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        // 네이버 응답은 'response' 안에 모든 정보가 들어있습니다.
        Map<String, Object> response = (Map<String, Object>) attributes.get(userNameAttributeName);

        String rawPhoneNumber = (String) response.get("mobile");
        String formattedPhoneNumber = normalizePhoneNumber(rawPhoneNumber);

        return OAuthAttributes.builder()
                .email((String) response.get("email"))
                .provider("naver")
                .providerId(String.valueOf(response.get("id"))) // 네이버 고유 ID
                .name((String) response.get("name"))
                .gender((String) response.get("gender"))
                .birthyear((String) response.get("birthyear"))
                .birthday(((String) response.get("birthday")).replace("-", ""))
                .phoneNumber(formattedPhoneNumber)
                .nameAttributeKey("id") // Principal 객체에서 이름을 참조할 때 사용할 키
                .attributes(response) // attributes 필드에는 response 맵 자체를 저장
                .build();
    }

    /**
     * 현재 객체의 정보를 바탕으로 데이터베이스에 저장하기 위한 {@link SignUpDto} 객체를 생성한다.
     * 사용자 ID는 "provider_providerId" 형식으로 조합되며, 소셜 로그인 사용자를 위한
     * 기본값(더미 패스워드, 약관 동의 등)을 설정한다.
     *
     * @return 데이터베이스 저장에 필요한 정보를 담은 {@link SignUpDto} 객체
     */
    public SignUpDto toSignUpDto() {
        return SignUpDto.builder()
                .userId(provider + "_" + providerId)
                .userName(this.name) // 사용자 이름 필드에 'name'을 직접 매핑
                .email(this.email)
                .phoneNumber(this.phoneNumber)
                .birthDate(this.birthyear + this.birthday)
                .genderCode(convertGenderToCode(this.gender))
                .password(provider + "_password") // 소셜 로그인이므로 더미 패스워드 설정
                .oauthProvider(provider)
                .oauthProviderId(providerId)
                // 약관 동의 등 기본값 설정
                .termsAgree1("Y")
                .termsAgree2("N")
                .termsAgree3("Y")
                .termsAgree4("Y")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 응답(male/female)을 DB 코드(M/F 등)로 변환하는 헬퍼 메소드
     *
     * @param gender 소셜 플랫폼에서 제공한 성별 문자열
     * @return 데이터베이스에 저장될 성별 코드 ("M", "F"), 또는 해당 없을 시 null
     */
    private String convertGenderToCode(String gender) {
        if ("M".equalsIgnoreCase(gender)) {
            return "M";
        } else if ("F".equalsIgnoreCase(gender)) {
            return "F";
        } else if ("male".equalsIgnoreCase(gender)) { // 카카오 호환
            return "M";
        } else if ("female".equalsIgnoreCase(gender)) { // 카카오 호환
            return "F";
        }
        return null; // 정보가 없거나 다른 값일 경우
    }

    /**
     * 국제 표준 형식의 전화번호(+82 10...)를 국내 형식(010...)으로 변환한다.
     * @param phoneNumber 카카오에서 받은 원본 전화번호
     * @return 정규화된 전화번호 (예: 01012345678)
     */
    private static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }

        // 숫자 외 모든 문자(공백, 하이픈 등) 제거
        String digitsOnly = phoneNumber.replaceAll("\\D", "");

        // 국가번호 '82'로 시작하는 경우 '0'으로 치환
        if (digitsOnly.startsWith("82")) {
            return "0" + digitsOnly.substring(2);
        }

        // 이미 010 등으로 시작하는 올바른 형식일 경우 그대로 반환
        return digitsOnly;
    }
}