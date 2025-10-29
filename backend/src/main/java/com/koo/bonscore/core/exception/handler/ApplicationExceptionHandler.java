package com.koo.bonscore.core.exception.handler;

import com.koo.bonscore.core.config.api.ApiResponse;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.response.ErrorResponse;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러 클래스
 *
 * @author bons
 * @version 1.0
 * @since 2025-01-13
 */

@RestControllerAdvice
public class ApplicationExceptionHandler {

    /**
     * 선언된 에러 처리
     *
     * @param ex        Exception
     * @param request   HTTP 요청과 관련된 정보를 제공하는 인터페이스
     */
    @ExceptionHandler(BsCoreException.class)
    public ResponseEntity<Object> handleCoreException(BsCoreException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),                                                            // timestamp : 발생시각
                ex.getStatusCode().getHttpStatus().value(),                                     // status: HTTP 상태 코드
                ex.getStatusCode().getHttpStatus().getReasonPhrase(),                           // error: HTTP 상태 설명
                ex.getErrorCode(),                                                              // code: 사용자 정의 에러 코드
                ex.getMessage(),                                                                // message: 예외 메시지
                request.getDescription(false).replace("uri=", "") // path: 요청 경로
        );

        ApiResponse<Object> apiResponse = ApiResponse.failure(
                ex.getErrorCode(),
                ex.getMessage(),
                errorResponse
        );

        return new ResponseEntity<>(apiResponse, ex.getStatusCode().getHttpStatus());
    }

    /**
     * @Valid 어노테이션을 사용한 DTO의 유효성 검사 실패 시 발생하는 예외를 처리.
     *
     * @param ex      MethodArgumentNotValidException
     * @param request WebRequest
     * @return        필드별 에러 메시지를 담은 ResponseEntity
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        BindingResult bindingResult = ex.getBindingResult();

        // 기본 메시지
        String representativeErrorMessage = "입력값이 유효하지 않습니다."; // 기본 메시지

        // 에러가 있다면 첫 번째 에러의 메시지를 대표 메시지로 설정
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                representativeErrorMessage = fieldError.getDefaultMessage();
            }
        }

        Map<String, String> errors = new HashMap<>();

        bindingResult.getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Object> apiResponse = ApiResponse.failure(
                ErrorCode.INVALID_INPUT.getCode(),
                representativeErrorMessage,
                errors
        );

        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 중복 로그인 / 토큰 관련 필터체인의 에러를 제어
     *
     * @param ex        JwtException
     * @param request   HTTP 요청과 관련된 정보를 제공하는 인터페이스
     * @return          ResponseEntity<Object>
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Object> handleJwtException(JwtException ex, WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),                                                            // timestamp : 발생시각
                HttpStatus.UNAUTHORIZED.value(),                                                // status: 기본 HTTP 상태 코드 (500)
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),                                      // error: 기본 HTTP 상태 설명
                ErrorCode.DUPLICATE_LOGIN.getCode(),                                            // code: 중복 로그인
                ex.getMessage(),                                                                // message: 예외 메시지
                request.getDescription(false).replace("uri=", "") // path: 요청 경로
        );

        ApiResponse<Object> apiResponse = ApiResponse.failure(
                ErrorCode.UNAUTHORIZED.getCode(),
                ex.getMessage(),
                errorResponse
        );

        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 권한제어 관련 에러
     *
     * @param ex        AccessDeniedException
     * @param request   HTTP 요청과 관련된 정보를 제공하는 인터페이스
     * @return          ResponseEntity<Object>
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),                                                            // timestamp : 발생시각
                HttpStatus.UNAUTHORIZED.value(),                                                // status: 기본 HTTP 상태 코드 (500)
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),                                      // error: 기본 HTTP 상태 설명
                ErrorCode.ACCESS_DENIED.getCode(),                                              // code: 중복 로그인
                ex.getMessage(),                                                                // message: 예외 메시지
                request.getDescription(false).replace("uri=", "") // path: 요청 경로
        );

        ApiResponse<Object> apiResponse = ApiResponse.failure(
                ErrorCode.UNAUTHORIZED.getCode(),
                ex.getMessage(),
                errorResponse
        );

        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 그 외의 예외 처리 (기타 모든 예외)
     *
     * @param ex        Exception
     * @param request   HTTP 요청과 관련된 정보를 제공하는 인터페이스
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex, WebRequest request) {
        // 일반적인 예외 처리
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),                                                            // timestamp : 발생시각
                HttpStatus.INTERNAL_SERVER_ERROR.value(),                                       // status: 기본 HTTP 상태 코드 (500)
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),                             // error: 기본 HTTP 상태 설명
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),                                      // code: 사용자 정의 에러 코드
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),                                   // message: 예외 메시지
                request.getDescription(false).replace("uri=", "") // path: 요청 경로
        );

        ApiResponse<Object> apiResponse = ApiResponse.failure(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                errorResponse
        );

        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
