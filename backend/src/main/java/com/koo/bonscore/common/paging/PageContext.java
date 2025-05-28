package com.koo.bonscore.common.paging;

public class PageContext {
    private static final ThreadLocal<Integer> totalCountHolder = new ThreadLocal<>();

    public static void setTotalCount(int count) {
        totalCountHolder.set(count);
    }

    public static Integer getTotalCount() {
        return totalCountHolder.get();
    }

    public static void clear() {
        totalCountHolder.remove();
    }
}
