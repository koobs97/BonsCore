package com.koo.bonscore.core.config.web.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Spring Security에서 인가(Authorization) 실패 시 (즉, @PreAuthorize 등에서 권한 부족)
 * 발생하는 AccessDeniedException을 처리하는 커스텀 핸들러.
 * 일관된 JSON 에러 응답을 반환하는 역할을 한다.
 */
@Slf4j
@Component // 이 클래스를 Spring Bean으로 등록하여 다른 곳에서 주입받을 수 있도록 함
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    // JSON 형식으로 응답을 변환하기 위해 ObjectMapper를 주입받음
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // 어떤 사용자가 어떤 경로에 권한 없이 접근했는지 로그를 남기는 것이 좋음
        log.warn("권한 없는 접근 시도 감지: {} {} - {}",
                request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());

        // 1. 응답할 에러 내용을 담은 ErrorResponse 객체 생성
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),                        // timestamp: 에러 발생 시각
                HttpStatus.FORBIDDEN.value(),               // status: 403
                HttpStatus.FORBIDDEN.getReasonPhrase(),     // error: "Forbidden"
                ErrorCode.ACCESS_DENIED.getCode(),          // code: "AUTH_003"
                ErrorCode.ACCESS_DENIED.getMessage(),       // message: "이 리소스에 접근할 수 있는 권한이 없습니다."
                request.getRequestURI()                     // path: 요청이 들어온 경로
        );

        // 2. HTTP 응답의 헤더 정보 설정
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // HTTP 상태 코드를 403으로 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // 응답의 Content-Type을 JSON으로 설정
        response.setCharacterEncoding("UTF-8"); // 문자 인코딩을 UTF-8로 설정

        // 3. 응답 본문(Body)에 JSON 데이터 쓰기
        // ObjectMapper를 사용해 ErrorResponse 객체를 JSON 문자열로 변환
        String jsonErrorResponse = objectMapper.writeValueAsString(errorResponse);
        // 클라이언트에게 JSON 응답을 보냄
        response.getWriter().write(jsonErrorResponse);
    }
}
