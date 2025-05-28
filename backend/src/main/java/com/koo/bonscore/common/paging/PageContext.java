package com.koo.bonscore.common.paging;

/**
 * <pre>
 * PageContext.java
 * 설명 : 페이징 처리를 위한 ThreadLocal 기반 컨텍스트 클래스.
 *        Mapper 또는 AOP에서 조회된 전체 건수를 담고, 서비스/컨트롤러에서 사용 가능하게 해준다.
 *
 * 사용 예:
 *   - MyBatis 인터셉터 또는 AOP에서 setTotalCount(count) 호출
 *   - 서비스에서 getTotalCount()로 값 조회
 *   - 요청 완료 후 반드시 clear() 호출하여 ThreadLocal 누수 방지
 *
 * 주의 : 요청/응답 단위로 값을 저장하므로, 요청 끝나고 반드시 clear() 해줘야 메모리 누수 방지됨.
 *        Interceptor 또는 AOP의 after-return 시점에서 clear() 호출 권장.
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-28
 */
public class PageContext {
    // 현재 쓰레드에서만 사용 가능한 totalCount 저장소
    private static final ThreadLocal<Integer> totalCountHolder = new ThreadLocal<>();
    /**
     * 페이징 전체 건수를 설정한다.
     * @param count 조회된 전체 건수
     */
    public static void setTotalCount(int count) {
        totalCountHolder.set(count);
    }
    /**
     * 현재 쓰레드의 전체 건수를 반환한다.
     * @return 설정된 전체 건수 또는 null
     */
    public static Integer getTotalCount() {
        return totalCountHolder.get();
    }
    /**
     * 현재 쓰레드에 저장된 전체 건수를 삭제한다.
     * 반드시 요청 처리 후 clear() 호출 필요.
     */
    public static void clear() {
        totalCountHolder.remove();
    }
}
