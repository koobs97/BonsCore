package com.koo.bonscore.biz.authorization.service;

import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.LogReqDto;
import com.koo.bonscore.biz.authorization.dto.res.LogResDto;
import com.koo.bonscore.biz.authorization.dto.res.MenuByRoleDto;
import com.koo.bonscore.biz.authorization.mapper.AuthorizationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <pre>
 * AuthorizationService.java
 * 설명 : 인가 (Authorization) 관련 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-01
 */
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final AuthorizationMapper authorizationMapper;

    /**
     * 권한에 맞는 메뉴 조회
     * @param userId
     * @return
     */
    @Transactional
    public List<MenuByRoleDto> getMenuByRole(AuthorizationDto request) {
        return authorizationMapper.getMenuByRole(request);
    }

    /**
     * 로그 조회 화면의 로그
     * @param request
     * @return
     */
    @Transactional
    public List<LogResDto> getUserLog(LogReqDto request) {
        return authorizationMapper.getUserLog(request);
    }
}
