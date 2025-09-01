package com.koo.bonscore.biz.authorization.mapper;

import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.LogReqDto;
import com.koo.bonscore.biz.authorization.dto.req.UpdateUserDto;
import com.koo.bonscore.biz.authorization.dto.res.LogResDto;
import com.koo.bonscore.biz.authorization.dto.res.MenuByRoleDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <pre>
 * AuthorizationMapper.java
 * 설명 : 인가 (Authorization) Mapper
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-01
 */
@Mapper
public interface AuthorizationMapper {

    /**
     * 권한에 따른 메뉴 조회
     * @param userId
     * @return
     */
    List<MenuByRoleDto> getMenuByRole(AuthorizationDto userId);

    /**
     * 활동 유형 조회
     * @param logReqDto
     * @return
     */
    List<LogResDto> getActivityType(LogReqDto logReqDto);

    /**
     * 결과 유형 조회
     * @param logReqDto
     * @return
     */
    List<LogResDto> getActivityResult(LogReqDto logReqDto);

    /**
     * 로그조회 화면의 로그
     * @param logReqDto
     * @return
     */
    List<LogResDto> getUserLog(LogReqDto logReqDto);

    /**
     * 유저 정보 업데이트
     * @param updateUserDto
     */
    void updateUserInfo(UpdateUserDto updateUserDto);
}
