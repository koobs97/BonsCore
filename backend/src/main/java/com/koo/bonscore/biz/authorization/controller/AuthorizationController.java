package com.koo.bonscore.biz.authorization.controller;

import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.LogReqDto;
import com.koo.bonscore.biz.authorization.dto.req.UpdateUserDto;
import com.koo.bonscore.biz.authorization.dto.res.ActivityResponseDto;
import com.koo.bonscore.biz.authorization.dto.res.LogResDto;
import com.koo.bonscore.biz.authorization.dto.res.MenuByRoleDto;
import com.koo.bonscore.biz.authorization.service.AuthorizationService;
import com.koo.bonscore.core.annotaion.PreventDoubleClick;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.log.annotaion.UserActivityLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <pre>
 * AuthorizationController.java
 * 설명 : 인가 (Authorization)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-01
 */
@RestController
@RequestMapping("api/authorization")
@RequiredArgsConstructor
public class AuthorizationController {
    private final AuthorizationService authorizationService;

    /**
     * 로그인 후 권한에 맞는 메뉴 조회
     * @param request UpdateUserDto
     * @param httpRequest HttpServletRequest
     * @param httpResponse HttpServletResponse
     * @throws Exception e
     * @return 메뉴 리스트
     */
    @UserActivityLog(activityType = "GET_MENUS", userIdField = "#request.userId")
    @PostMapping("/getMenus")
    public List<MenuByRoleDto> getMenus(@RequestBody AuthorizationDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        try {
            return authorizationService.getMenuByRole(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 검색조건에 쓰일 코드 조회
     * @param request UpdateUserDto
     * @param httpRequest HttpServletRequest
     * @param httpResponse HttpServletResponse
     * @throws Exception e
     * @return 코드 리스트
     */
    @PostMapping("/geActivityCds")
    public ActivityResponseDto geActivityCds(@RequestBody LogReqDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        try {
            return authorizationService.getActivityCd(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 로그 데이터 조회
     * @param request UpdateUserDto
     * @param httpRequest HttpServletRequest
     * @param httpResponse HttpServletResponse
     * @throws Exception e
     * @return 로그리스트
     */
    @UserActivityLog(activityType = "GET_LOGS", userIdField = "#request.userId")
    @PostMapping("/getLogs")
    public List<LogResDto> getLogs(@RequestBody LogReqDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        try {
            return authorizationService.getUserLog(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 유저 정보 업데이트
     * @param request UpdateUserDto
     * @param httpRequest HttpServletRequest
     * @param httpResponse HttpServletResponse
     * @throws Exception e
     */
    @UserActivityLog(activityType = "UPDATE_USER", userIdField = "#request.userId")
    @PostMapping("/updateUserInfo")
    public void updateUserInfo(@RequestBody UpdateUserDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        try {
            authorizationService.updateUserInfo(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 현재 비밀번호 확인
     *
     * @param request UpdateUserDto
     * @param httpRequest HttpServletRequest
     * @param httpResponse HttpServletResponse
     * @return Boolean result
     * @throws Exception e
     */
    @UserActivityLog(activityType = "VALIDATE_PASSWORD", userIdField = "#request.userId")
    @PostMapping("/validatePassword")
    public boolean validatePassword(@RequestBody UpdateUserDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        try {
            return authorizationService.passwordValidate(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 비밀번호 업데이트
     * 
     * @param request UpdateUserDto
     * @param httpRequest HttpServletRequest
     * @param httpResponse HttpServletResponse
     * @throws Exception e
     */
    @UserActivityLog(activityType = "UPDATE_PASSWORD_AF_LOGIN", userIdField = "#request.userId")
    @PostMapping("/updatePassword")
    public void updatePassword(@RequestBody UpdateUserDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        try {
            authorizationService.updatePassword(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }
}
