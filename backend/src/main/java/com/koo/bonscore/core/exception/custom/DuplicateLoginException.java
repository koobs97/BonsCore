package com.koo.bonscore.core.exception.custom;

import com.koo.bonscore.core.exception.enumType.ErrorCode;

/**
 * <pre>
 * DuplicateLoginException.java
 * 설명 : 중복 로그인이 감지되었을 때 발생하는 예외 클래스.
 * </pre>
 *
 * @author bons
 * @version 1.0
 * @see BsCoreException
 * @see ErrorCode
 * @since 2025-11-14
 */
public class DuplicateLoginException extends BsCoreException {

    /**
     * 기본 생성자
     */
    public DuplicateLoginException() {
        super(ErrorCode.DUPLICATE_LOGIN);
    }

    /**
     * 사용자 정의 메시지를 포함하는 생성자
     * @param message 예외와 함께 전달할 상세 메시지
     */
    public DuplicateLoginException(String message) {
        super(ErrorCode.DUPLICATE_LOGIN, message);
    }
}
