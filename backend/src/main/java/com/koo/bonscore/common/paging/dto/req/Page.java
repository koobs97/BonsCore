package com.koo.bonscore.common.paging.dto.req;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 * Page.java
 * 설명 : 페이징 정보.
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-28
 */
@Getter
@Setter
public class Page {
    private int pageNum;    // 1-based
    private int pageSize;
    private boolean enablePaging;

    public Page(int pageNum, int pageSize) {
        this.pageNum = Math.max(pageNum, 1);
        this.pageSize = pageSize;
    }

    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }

    public int getLimit() {
        return pageSize;
    }
}
