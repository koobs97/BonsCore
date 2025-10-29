package com.koo.bonscore.biz.auth.mapper;

import com.koo.bonscore.biz.auth.dto.LoginCheckDto;
import com.koo.bonscore.biz.auth.dto.LoginHistoryDto;
import com.koo.bonscore.biz.auth.dto.UserDto;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.auth.dto.req.UserInfoSearchDto;
import org.apache.catalina.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <pre>
 * AuthMapper.java
 * 설명 : 권한 관련 Mapper 인터페이스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-07-22
 */

@Mapper
public interface AuthMapper {
    
    /**
     * 로그인
     * @param loginDto 사용자 ID
     * @return 해싱된 패스워드, 휴먼계정여부
     */
    LoginCheckDto login(LoginDto loginDto);

    /**
     * 유저 ID로 사용자 정보 조회
     * @param loginDto 사용자 ID
     * @return 유저 정보
     */
    UserDto findByUserId(LoginDto loginDto);

    /**
     * 유저 권한 조회
     * @param userId 사용자 ID
     * @return 유저 권한
     */
    List<String> findRoleByUserId(String userId);

    /**
     * 사용자의 최근 접속 국가 목록 조회
     * @param userId 사용자 ID
     * @return 사용자의 최근 접속 국가 목록
     */
    List<String> findRecentLoginCountries(String userId);

    /**
     * 비정상 로그인으로 계정잠금
     * @param loginDto 로그인 시도 정보
     */
    void updateRequiresYn(LoginDto loginDto);

    /**
     * 로그인일시 업데이트
     * @param loginDto 사용자 ID
     */
    void updateLoginAt(LoginDto loginDto);

    /**
     * 로그인 기록 저장
     * @param history 사용자 접속 기록
     */
    void insertLoginHistory(LoginHistoryDto history);

    /**
     * 아이디 중복 체크
     * @param signUpDto 사용자 ID
     * @return 사용중인 ID 개수
     */
    Integer existsById(SignUpDto signUpDto);

    /**
     * 이메일 중복 체크
     * @param signUpDto email
     * @return 사용중인 email 개수
     */
    Integer existsByEmail(SignUpDto signUpDto);

    /**
     * 회원가입
     * @param signUpDto 회원가입정보
     */
    void signUpUser(SignUpDto signUpDto);

    /**
     * 회원가입 시 일반사용자 권한부여
     * @param signUpDto 사용자 ID
     */
    void signUpUserRole(SignUpDto signUpDto);

    /**
     * 유저명, 이메일로 유효한 사용자인지 조회
     * @param userInfoSearchDto email
     * @return 사용자 이름
     */
    String findByUserNameAndEmail(UserInfoSearchDto userInfoSearchDto);

    /**
     * 사용자 ID 조회
     * @param userInfoSearchDto email
     * @return 사용자 ID
     */
    String findUserIdByMail(UserInfoSearchDto userInfoSearchDto);

    /**
     * 사용자 아이디로 유저명, 이메일 조회
     * @param userInfoSearchDto 사용자 ID
     * @return 사용자 이름, 이메일
     */
    UserInfoSearchDto findUserById(UserInfoSearchDto userInfoSearchDto);

    /**
     * 사용자가 입력한 유저명, 이메일이 맞는지 조회
     * @param userInfoSearchDto 사용자 이름, 이메일
     * @return 사용자 ID, 이메일
     */
    UserInfoSearchDto findUserByNameMail(UserInfoSearchDto userInfoSearchDto);

    /**
     * 사용자 아이디로 보안질문 조회
     * @param userInfoSearchDto 유저 ID
     * @return 질문텍스트
     */
    String findPasswordHintById(UserInfoSearchDto userInfoSearchDto);

    /**
     * 보안질문 정답 조회
     * @param userInfoSearchDto 유저 ID
     * @return 정답
     */
    String findHintAnswerById(UserInfoSearchDto userInfoSearchDto);

    /**
     * 비밀번호 업데이트
     * @param userInfoSearchDto 아이디
     */
    void updatePassword(UserInfoSearchDto userInfoSearchDto);

    /**
     * 계정잠김 해제
     * @param userInfoSearchDto 아이디
     */
    void updateUnLocked(UserInfoSearchDto userInfoSearchDto);
}
