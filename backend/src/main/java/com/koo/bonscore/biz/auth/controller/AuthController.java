package com.koo.bonscore.biz.auth.controller;

import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.res.LoginResponseDto;
import com.koo.bonscore.biz.auth.dto.res.RefreshTokenDto;
import com.koo.bonscore.biz.auth.service.AuthService;
import com.koo.bonscore.core.annotaion.PreventDoubleClick;
import com.koo.bonscore.core.config.api.ApiResponse;
import com.koo.bonscore.core.config.web.security.config.JwtTokenProvider;
import com.koo.bonscore.core.config.web.security.config.LoginSessionManager;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginSessionManager loginSessionManager;

    @PreventDoubleClick
    @PostMapping("/login")
    public LoginResponseDto login(@RequestBody LoginDto request, HttpServletResponse httpResponse) throws Exception {

        // 1. 사용자 ID로 중복 로그인 확인
        // 강제 로그인이 아니고, 이미 로그인된 세션이 있다면
        if (!request.isForce() && loginSessionManager.isDuplicateLogin(request.getUserId())) {
            LoginResponseDto responseDto = new LoginResponseDto();
            responseDto.setSuccess(false);
            responseDto.setReason("DUPLICATE_LOGIN"); // 프론트와 약속된 이유 전달
            responseDto.setMessage("다른 기기에서 로그인 중입니다.<br>접속을 강제로 끊고 로그인하시겠습니까?");
            return responseDto;
        }

        // 실제 로그인 처리
        LoginResponseDto responseDto = authService.login(request);

        // 로그인 성공 시 세션 처리
        if (responseDto.getSuccess()) {
            String userId = request.getUserId();
            String accessToken = responseDto.getAccessToken();
            

            // 새로운 세션 등록 (이 과정에서 기존 세션은 블랙리스트 처리됨
            loginSessionManager.registerSession(userId, accessToken);

            // Refresh Token 쿠키로 전달
            ResponseCookie cookie = ResponseCookie.from("refresh_token", responseDto.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(JwtTokenProvider.REFRESH_TOKEN_VALIDITY / 1000)
                    .build();

            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            // 보안상 응답 body에는 refreshToken 제거
            responseDto.setRefreshToken(null);
        }

        return responseDto;
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Object>> refreshAccessToken(@RequestBody RefreshTokenDto refreshTokenDto, HttpServletRequest request) {
        String refreshToken = refreshTokenDto.getRefreshToken();

        // Refresh Token 검증
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            ErrorResponse errorResponse = new ErrorResponse(
                    LocalDateTime.now(),                                                            // timestamp : 발생시각
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),                                       // status: 기본 HTTP 상태 코드 (500)
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),                             // error: 기본 HTTP 상태 설명
                    ErrorCode.INVALID_REFRESH_TOKEN.getCode(),                                      // code: 사용자 정의 에러 코드
                    ErrorCode.INVALID_REFRESH_TOKEN.getMessage(),                                   // message: 예외 메시지
                    "/api/refresh" // path: 요청 경로
            );
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure(ErrorCode.INVALID_REFRESH_TOKEN.getCode(), "접속권한이 없습니다.", errorResponse));
        }

        // Refresh Token에서 사용자 정보 추출
        String userId = jwtTokenProvider.getUserId(refreshToken);

        // 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.createToken(userId, JwtTokenProvider.ACCESS_TOKEN_VALIDITY);

        // 새로운 Access Token 반환
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", newAccessToken));
    }

}
