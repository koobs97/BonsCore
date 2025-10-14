package com.koo.bonscore.biz.authorization.mapper;

import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.LogReqDto;
import com.koo.bonscore.biz.authorization.dto.req.UpdateUserDto;
import com.koo.bonscore.biz.authorization.dto.req.UserReqDto;
import com.koo.bonscore.biz.authorization.dto.res.LogResDto;
import com.koo.bonscore.biz.authorization.dto.res.MenuByRoleDto;
import com.koo.bonscore.biz.authorization.dto.res.SecurityQuestionDto;
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
     * @param userId AuthorizationDto
     * @return List<MenuByRoleDto>
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
     * @return 활동 유형 리스트
     */
    List<LogResDto> getActivityType();

    /**
     * 결과 유형 조회
     * @return 결과 유형 리스트
     */
    List<LogResDto> getActivityResult();

    /**
     * 로그조회 화면의 로그
     * @param logReqDto 검색조건
     * @return 로그
     */
    List<LogResDto> getUserLog(LogReqDto logReqDto);

    /**
     * 유저 정보 업데이트
     * @param updateUserDto 업데이트할 정보
     */
    void updateUserInfo(UpdateUserDto updateUserDto);

    /**
     * 비밀번호 변경을 위해 현재 비밀번호 조회
     * @param updateUserDto 유저 ID
     * @return Password
     */
    String getPassword(UpdateUserDto updateUserDto);

    /**
     * 비밀번호 변경
     * @param updateUserDto 업데이트할 정보(비밀번호)
     */
    void updatePassword(UpdateUserDto updateUserDto);

    /**
     * 보안질문 리스트 조회
     * @return 보안질문 리스트
     */
    List<SecurityQuestionDto> getSecurityQuestion();

    /**
     * 비밀번호 질문 및 답변 입력
     * @param updateUserDto 업데이트할 정보(비밀번호 질문 및 답변)
     */
    void updateHintWithAns(UpdateUserDto updateUserDto);

    /**
     * 회원 탈퇴
     * @param updateUserDto 탈퇴할 유저 ID
     */
    void updateWithdrawn(UpdateUserDto updateUserDto);
}
