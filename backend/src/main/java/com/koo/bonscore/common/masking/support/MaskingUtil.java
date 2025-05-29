package com.koo.bonscore.common.masking.support;

public class MaskingUtil {

    public static String maskName(String name) {
        if (name == null || name.length() <= 1) return "*";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "";
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        String maskedLocal = local.charAt(0) + "*".repeat(local.length() - 1);
        return maskedLocal + "@" + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
