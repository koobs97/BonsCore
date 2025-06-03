package com.koo.bonscore.common.masking.util;

/**
 * <pre>
 * MaskingUtil.java
 * 설명 : 마스킹 유틸 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-06-03
 */
public class MaskingUtil {

    /**
     * 이름 마스킹
     *
     * @param name 이름 파라미터
     * @return 마스킹결과
     */
    public static String maskName(String name) {
        if (name == null || name.length() <= 1) return "*";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /**
     * 이메일 마스킹
     *
     * @param email 이메일 파라미터
     * @return 마스킹결과
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "";
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        String maskedLocal = local.charAt(0) + "*".repeat(local.length() - 1);
        return maskedLocal + "@" + domain;
    }

    /**
     * 전화번호 마스킹
     * @param phone 전화번호 파라미터
     * @return 마스킹결과
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
