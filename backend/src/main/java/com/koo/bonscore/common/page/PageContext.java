package com.koo.bonscore.common.page;

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
