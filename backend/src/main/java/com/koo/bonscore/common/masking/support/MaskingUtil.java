package com.koo.bonscore.common.masking.support;

public class MaskingUtil {

    public static String mask(String value, MaskingType type) {
        if (value == null) return null;

        return switch (type) {
            case NAME -> value.charAt(0) + "*".repeat(value.length() - 1);
            case EMAIL -> {
                int idx = value.indexOf("@");
                yield idx <= 1 ? "*".repeat(value.length()) :
                        value.charAt(0) + "****" + value.substring(idx);
            }
        };
    }
}
