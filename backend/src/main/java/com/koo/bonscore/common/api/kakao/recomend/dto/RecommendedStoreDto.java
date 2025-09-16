package com.koo.bonscore.common.api.kakao.recomend.dto;


import com.koo.bonscore.common.api.kakao.surround.dto.KakaoMapResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedStoreDto {
    private String name;
    private String address;
    private String category;
    private String placeUrl;

    // KakaoMapResponse.Document 객체를 RecommendedStoreDto로 변환하는 정적 팩토리 메서드
    public static RecommendedStoreDto from(KakaoMapResponse.Document document) {
        return new RecommendedStoreDto(
                document.getPlaceName(),
                document.getAddressName(),
                document.getCategoryName(),
                document.getPlaceUrl()
        );
    }
}
