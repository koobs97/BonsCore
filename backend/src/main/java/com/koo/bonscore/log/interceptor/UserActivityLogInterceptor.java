package com.koo.bonscore.log.interceptor;

import com.koo.bonscore.log.dto.UserActivityLogDto;
import com.koo.bonscore.log.service.UserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.koo.bonscore.common.util.web.WebUtils;

/**
 * <pre>
 * UserActivityLogInterceptor.java
 * 설명 : 사용자 활동 로그를 최종적으로 수집하고 기록하는 HandlerInterceptor
 * </pre>
 *
 * <p>
 * 이 인터셉터는 컨트롤러의 요청 처리가 완료된 후({@code afterCompletion}) 동작한다.
 * {@code UserActivityLogAspect}에 의해 {@code HttpServletRequest}에 미리 저장된
 * 활동 유형(activityType)과 사용자 ID(userId) 등의 정보를 바탕으로 로그 데이터를 조립한다.
 * </p>
 * <p>
 * 또한, 요청 처리 중 발생한 예외({@code Exception ex})나 컨트롤러가 직접 설정한
 * 비즈니스 결과(activityResult)를 종합하여 최종 활동 결과(SUCCESS/FAILURE)를 결정한다.
 * 완성된 로그 데이터는 {@link UserActivityLogService}를 통해 비동기적으로 DB에 저장된다.
 * </p>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-07-23
 *
 * @see com.koo.bonscore.log.aop.UserActivityLogAspect
 * @see com.koo.bonscore.log.service.UserActivityLogService
 * @see com.koo.bonscore.core.config.web.WebConfig
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityLogInterceptor implements HandlerInterceptor {

    private final UserActivityLogService userActivityLogService;

    /**
     * 컨트롤러의 메소드 실행 및 뷰 렌더링까지 모두 완료된 후에 호출되는 메소드.
     * <p>
     * 이 시점에서는 요청 처리 중 발생한 예외를 파라미터로 받을 수 있어,
     * 활동의 최종 성공/실패 여부를 판단하기에 가장 적합.
     * </p>
     *
     * @param request  현재 HTTP 요청 객체.
     * @param response 현재 HTTP 응답 객체.
     * @param handler  요청을 처리한 핸들러(컨트롤러 메소드 등).
     * @param ex       요청 처리 중 발생한 예외. 예외가 없으면 {@code null}.
     * @throws Exception 인터셉터 자체에서 발생하는 예외.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 컨트롤러에서 설정한 'activityType' 속성을 가져옵니다.
        String activityType = (String) request.getAttribute("activityType");

        // 이 속성이 설정된 요청만 로깅 대상으로 간주합니다.
        if (activityType == null) {
            return;
        }

        // 컨트롤러에서 설정한 사용자 ID를 가져옵니다. (로그인/회원가입 시도 ID)
        String userId = (String) request.getAttribute("userId");

        String activityResult = (String) request.getAttribute("activityResult");
        String errorMessage = (String) request.getAttribute("errorMessage");

        // 1. 컨트롤러가 명시적으로 결과를 설정한 경우, 그 값을 최우선으로 사용
        if (activityResult == null) {
            // 2. 명시적 설정이 없으면, 예외 발생 여부로 판단
            activityResult = (ex == null) ? "SUCCESS" : "FAILURE";
            if (ex != null) {
                errorMessage = ex.getMessage();
            }
        }

        UserActivityLogDto logDto = UserActivityLogDto.builder()
                .userId(userId)
                .activityType(activityType)
                .activityResult(activityResult) // 예외가 없으면 성공, 있으면 실패
                .requestIp(WebUtils.getClientIP(request))
                .requestUri(request.getRequestURI())
                .requestMethod(request.getMethod())
                .errorMessage(errorMessage) // 예외가 있으면 메시지 기록
                .userAgent(request.getHeader("User-Agent"))
                .build();

        // 비동기 로그 저장 서비스 호출
        userActivityLogService.saveLog(logDto);
    }
}
