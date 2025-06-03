package com.koo.bonscore.common.masking.context;

/**
 * <pre>
 * MaskingContext.java
 * 설명 : JSON 응답 시 마스킹 적용 여부를 동적으로 제어하기 위한 ThreadLocal 기반의 컨텍스트 클래스.
 *       각 HTTP 요청마다 독립적인 마스킹 설정을 유지할 수 있도록 ThreadLocal을 사용함.
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-06-02
 */
public class MaskingContext {

    /**
     * ThreadLocal을 사용하여 현재 요청의 마스킹 설정 상태를 저장
     * 기본값은 true (마스킹 적용)
     */
    private static final ThreadLocal<Boolean> MASKING_ENABLED = ThreadLocal.withInitial(() -> true); // 기본값 true

    /**
     * 현재 스레드에 대해 마스킹 여부 설정
     *
     * @param enabled true일 경우 마스킹 적용, false일 경우 마스킹 해제
     */
    public static void setMaskingEnabled(boolean enabled) {
        MASKING_ENABLED.set(enabled);
    }

    /**
     * 현재 스레드의 마스킹 설정값 조회
     *
     * @return true일 경우 마스킹 적용, false일 경우 마스킹 해제
     */
    public static boolean isMaskingEnabled() {
        return MASKING_ENABLED.get();
    }

    /**
     * 현재 스레드에 저장된 마스킹 설정값 제거
     * 반드시 요청 처리 완료 후 호출하여 메모리 누수 방지
     */
    public static void clear() {
        MASKING_ENABLED.remove();
    }
}
