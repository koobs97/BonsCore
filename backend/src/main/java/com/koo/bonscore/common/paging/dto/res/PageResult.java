package com.koo.bonscore.common.paging.dto.res;

import java.util.List;

public class PageResult<T> {
    private List<T> items;
    private int total;
}
