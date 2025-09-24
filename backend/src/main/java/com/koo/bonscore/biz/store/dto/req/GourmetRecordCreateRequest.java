package com.koo.bonscore.biz.store.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class GourmetRecordCreateRequest {
    // GOURMET_RECORDS 테이블 컬럼과 매칭
    private String name;       // STORE_NAME
    private String category;
    private String visitDate;
    private int rating;
    private String memo;
    private String referenceUrl;

    // GOURMET_IMAGES 정보
    private List<GourmetImageDto> images;

    // (중요) 서비스 로직에서 사용할 추가 필드
    private Long recordId; // MyBatis에서 INSERT 후 ID를 받아오기 위함
    private String userId;   // JWT 토큰 등에서 추출한 사용자 ID

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
