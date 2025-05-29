package com.koo.bonscore.common.paging.dto.res;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 * PageData.java
 * 설명 : 페이징 처리에 필요한 데이터 결과값
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-29
 */
@Getter
@Setter
public class PageData {
    private int page;            // 현재 페이지 번호
    private int size;            // 페이지당 개수
    private long totalCount;     // 전체 건수
    private int totalPages;      // 전체 페이지 수
    private long  startRow;      // 시작 row 번호
    private long  endRow;        // 끝 row 번호
    private boolean hasNext;     // 다음 페이지 존재 여부
    private boolean hasPrevious; // 이전 페이지 존재 여부

    public PageData(int page, int size, long totalCount) {
        this.totalCount = totalCount;
        this.page = page;
        this.size = size;

        this.totalPages = (int) Math.ceil((double) totalCount / size);
        this.startRow = (long) (page - 1) * size + 1;
        this.endRow = Math.min(startRow + size - 1, totalCount);
        this.hasPrevious = page > 1;
        this.hasNext = page < totalPages;
    }
}
