package com.koo.bonscore.biz.store.dto.res;

import com.koo.bonscore.biz.store.dto.req.GourmetImageDto;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class GourmetRecordDto {
    // Frontend에서 사용하는 필드명과 일치시키는 것이 좋습니다.
    private Long id;              // recordId
    private String name;          // storeName
    private String category;
    private LocalDate visitDate;
    private int rating;
    private String memo;
    private String referenceUrl;

    // 자식 테이블(이미지) 정보
    private List<GourmetImageDto> images;
}
