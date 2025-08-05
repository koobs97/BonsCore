package com.koo.bonscore.biz.authorization.controller;

import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.LogReqDto;
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

    @UserActivityLog(activityType = "GET_MENUS", userIdField = "#request.userId")
    @PostMapping("/getMenus")
    public List<MenuByRoleDto> getMenus(@RequestBody AuthorizationDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        try {
            return authorizationService.getMenuByRole(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            if (e instanceof BsCoreException)
                throw (BsCoreException) e;
            else
                throw new RuntimeException(e);
        }
    }

    @UserActivityLog(activityType = "GET_LOGS", userIdField = "#request.userId")
    @PostMapping("/getLogs")
    public List<LogResDto> getLogs(@RequestBody LogReqDto request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        try {
            return authorizationService.getUserLog(request);
        } catch (Exception e) {
            httpRequest.setAttribute("activityResult", "FAILURE");
            httpRequest.setAttribute("errorMessage", e.getMessage());
            if (e instanceof BsCoreException)
                throw (BsCoreException) e;
            else
                throw new RuntimeException(e);
        }
    }
}
