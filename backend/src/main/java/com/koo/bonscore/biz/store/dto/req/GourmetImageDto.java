package com.koo.bonscore.biz.store.dto.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GourmetImageDto {
    private Long imageId;
    private String originalFileName;
    private String storedFileName;
    private String imageUrl;
    private Long fileSize;

    public void setRecordId(Long recordId) {

    }
}
