package com.koo.bonscore.biz.authorization.controller;

import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.LogReqDto;
import com.koo.bonscore.biz.authorization.dto.req.UpdateUserDto;
import com.koo.bonscore.biz.authorization.dto.req.UserReqDto;
import com.koo.bonscore.biz.authorization.dto.res.*;
import com.koo.bonscore.biz.authorization.service.AuthorizationService;
import com.koo.bonscore.log.annotaion.UserActivityLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
     *
     * @param userDetails 인증된 사용자 정보
     * @param httpRequest HttpServletRequest
     * @param httpResponse HttpServletResponse
     * @throws Exception e
     * @return 메뉴 리스트
     */
    @UserActivityLog(activityType = "GET_MENUS")
    @GetMapping("/menus")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public List<MenuByRoleDto> getMenus(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        try {
            AuthorizationDto request = new AuthorizationDto(userDetails.getUsername());
            return authorizationService.getMenuByRole(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 사용자 관리 사용자 정보 조회
     *
     * @param request UserReqDto (query string으로 바인딩)
     * @param httpRequest HttpServletRequest
     * @param httpResponse HttpServletResponse
     * @throws Exception e
     * @return 사용자 리스트
     */
    @UserActivityLog(activityType = "GET_USER_INFOS")
    @GetMapping("/users")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public List<UserResDto> getUserInfos(@ModelAttribute UserReqDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        try {
            return authorizationService.getUserInfos(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 검색조건에 쓰일 코드 조회
     *
     * @param request LogReqDto (query string으로 바인딩)
     * @param httpRequest HttpServletRequest
     * @param httpResponse HttpServletResponse
     * @throws Exception e
     * @return 코드 리스트
     */
    @GetMapping("/logs/activity-codes")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ActivityResponseDto getActivityCodes(@ModelAttribute LogReqDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
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
     *
     * @param request LogReqDto (query string으로 바인딩)
     * @param httpRequest HttpServletRequest
     * @param httpResponse HttpServletResponse
     * @throws Exception e
     * @return 로그리스트
     */
    @UserActivityLog(activityType = "GET_LOGS")
    @GetMapping("/logs")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public List<LogResDto> getLogs(@ModelAttribute LogReqDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
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
     *
     * @param request UpdateUserDto
     * @param httpRequest HttpServletRequest
     * @param httpResponse HttpServletResponse
     * @throws Exception e
     */
    @UserActivityLog(activityType = "UPDATE_USER")
    @PatchMapping("/users")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
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
     * @return Boolean result
     * @throws Exception e
     */
    @UserActivityLog(activityType = "VALIDATE_PASSWORD")
    @PostMapping("/users/password/validate")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public boolean validatePassword(@RequestBody UpdateUserDto request, @AuthenticationPrincipal UserDetails userDetail, HttpServletRequest httpRequest) throws Exception {
        try {
            request.setUserId(userDetail.getUsername());
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
     * @throws Exception e
     */
    @UserActivityLog(activityType = "UPDATE_PASSWORD_AF_LOGIN")
    @PatchMapping("/users/password")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public void updatePassword(@RequestBody UpdateUserDto request, HttpServletRequest httpRequest) throws Exception {
        try {
            authorizationService.updatePassword(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 보안질문 리스트 조회
     *
     * @param httpRequest HTTP 요청 객체
     * @return 보안질문 리스트
     */
    @GetMapping("/security-questions")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public List<SecurityQuestionDto> getSecurityQuestion(HttpServletRequest httpRequest) {
        try {
            return authorizationService.getSecurityQuestion();
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 비밀번호 질문 및 답변 입력
     *
     * @param request UpdateUserDto
     * @param httpRequest HttpServletRequest
     * @throws Exception e
     */
    @UserActivityLog(activityType = "UPDATE_PASSWORD_HINT")
    @PatchMapping("/users/hint")
    public void updateHintWithAns(@RequestBody UpdateUserDto request, @AuthenticationPrincipal UserDetails userDetail, HttpServletRequest httpRequest) throws Exception {
        try {
            request.setUserId(userDetail.getUsername());
            authorizationService.updateHintWithAns(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }

    /**
     * 회원탈퇴
     *
     * @param userDetail Security 유저정보
     * @param httpRequest HttpServletRequest
     * @param httpResponse HttpServletResponse
     * @throws Exception e
     */
    @UserActivityLog(activityType = "UPDATE_WITHDRAWN")
    @DeleteMapping("/users/me")
    public void updateWithdrawn(@AuthenticationPrincipal UserDetails userDetail, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        try {
            authorizationService.updateWithdrawn(userDetail.getUsername());
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            throw (e);
        }
    }
}
