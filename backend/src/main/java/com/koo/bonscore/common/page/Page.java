package com.koo.bonscore.common.page;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Page {
    private int pageNum;    // 1-based
    private int pageSize;

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
