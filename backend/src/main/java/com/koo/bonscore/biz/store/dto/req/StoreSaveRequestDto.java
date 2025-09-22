package com.koo.bonscore.biz.store.dto.req;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * 맛집 정보 생성을 위한 요청 DTO
 */
@Getter
@Setter
public class StoreSaveRequestDto {
    private String name;
    private String category;
    private int rating;
    private String visitDate; // "YYYY-MM-DD" 형식의 문자열
    private String memo;

    // 업로드가 완료된 이미지 파일들의 정보를 담는 리스트
    private List<ImageInfo> images;

    /**
     * 첨부된 이미지 파일 1개의 정보를 담는 내부 클래스
     */
    @Getter
    @Setter
    public static class ImageInfo {
        private String imageUrl;
        private String originalFileName;
        private String storedFileName;
        private long fileSize;
    }
}
