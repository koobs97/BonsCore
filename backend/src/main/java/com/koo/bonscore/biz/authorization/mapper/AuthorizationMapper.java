package com.koo.bonscore.biz.authorization.mapper;

import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.LogReqDto;
import com.koo.bonscore.biz.authorization.dto.req.UpdateUserDto;
import com.koo.bonscore.biz.authorization.dto.req.UserReqDto;
import com.koo.bonscore.biz.authorization.dto.res.LogResDto;
import com.koo.bonscore.biz.authorization.dto.res.MenuByRoleDto;
import com.koo.bonscore.biz.authorization.dto.res.UserResDto;
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
     * 사용자 관리 정보 조회
     * @param userReqDto 검색조건
     * @return 사용자 정보 리스트
     */
    List<UserResDto> getUserInfos(UserReqDto userReqDto);

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

    /**
     * 비밀번호 변경을 위해 현재 비밀번호 조회
     * @param updateUserDto UpdateUserDto
     * @return Password
     */
    String getPassword(UpdateUserDto updateUserDto);

    /**
     * 비밀번호 변경
     * @param updateUserDto UpdateUserDto
     */
    void updatePassword(UpdateUserDto updateUserDto);
}
