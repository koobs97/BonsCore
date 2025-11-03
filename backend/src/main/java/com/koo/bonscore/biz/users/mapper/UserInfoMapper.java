package com.koo.bonscore.biz.users.mapper;

import com.koo.bonscore.biz.users.dto.DormantUserInfoDto;
import com.koo.bonscore.biz.users.dto.res.UserInfoDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <pre>
 * UserInfoMapper.java
 * 설명 : 사용자 정보 관리 매퍼 인터페이스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-01
 */
@Mapper
public interface UserInfoMapper {

    /**
     * 사용자 정보 조회
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    UserInfoDto getUserInfo(String userId);

    /**
     * 사용자 권한 조회
     * @param userId 사용자 ID
     * @return 사용자 권한 정보
     */
    UserInfoDto getUserRole(String userId);

    /**
     * 휴면전환 30일전 안내메일 발송대상 조회
     * @return 휴면전환 예정자들
     */
    List<DormantUserInfoDto> getUsersForDormancyNotice();

    /**
     * 휴먼대상 계정 조회
     * @return 휴먼대상 계정 리스트
     */
    List<DormantUserInfoDto> getDormantUsers();

    /**
     * 휴먼대상 계정 분리
     * @param dormantUsers 휴먼대상 계정 리스트
     */
    void insertDormantUsers(@Param("users") List<DormantUserInfoDto> dormantUsers);

    /**
     * 원본 테이블의 개인정보 파기 및 상태 업데이트
     * @param userIds 파기대상 계정들
     */
    void updateUsersToDormantState(List<String> userIds);

    /**
     * 휴면 테이블에서 특정 사용자 정보를 조회
     * @param userId 사용자 ID
     * @return 분리 보관된 사용자 정보
     */
    DormantUserInfoDto getDormantUserInfo(String userId);

    /**
     * 분리 보관된 정보로 원본 사용자 테이블을 복원
     * @param dormantUserInfo 복원할 사용자 정보
     */
    void restoreUserFromDormancy(DormantUserInfoDto dormantUserInfo);

    /**
     * 휴면 테이블에서 특정 사용자 정보를 삭제
     * @param userId 사용자 ID
     */
    void deleteDormantUserInfo(String userId);
}
