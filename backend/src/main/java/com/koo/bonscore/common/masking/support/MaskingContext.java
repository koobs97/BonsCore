package com.koo.bonscore.common.masking.support;

public class MaskingContext {
    private static final ThreadLocal<Boolean> maskingEnabled = ThreadLocal.withInitial(() -> true);

    public static void enableMasking() {
        maskingEnabled.set(true);
    }

    public static void disableMasking() {
        maskingEnabled.set(false);
    }

    public static boolean isMaskingEnabled() {
        return maskingEnabled.get();
    }

    public static void clear() {
        maskingEnabled.remove();
    }
}
