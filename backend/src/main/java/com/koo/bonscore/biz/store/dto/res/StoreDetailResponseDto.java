package com.koo.bonscore.biz.store.dto.res;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * 맛집 상세 정보 조회를 위한 응답 DTO
 */
@Getter
@Setter
public class StoreDetailResponseDto {
    private Long id;
    private String name;
    private String category;
    private int rating;
    private String visitDate;
    private String memo;
    private List<ImageDto> images; // 해당 맛집에 연관된 이미지 목록

    /**
     * 상세 정보에 포함될 이미지 1개의 정보를 담는 내부 DTO
     */
    @Getter
    @Setter
    public static class ImageDto {
        private Long id;
        private String imageUrl;
        private String originalFileName;
    }
}
