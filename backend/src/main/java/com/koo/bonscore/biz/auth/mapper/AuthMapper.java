package com.koo.bonscore.biz.auth.mapper;

import com.koo.bonscore.biz.auth.dto.UserDto;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import org.apache.ibatis.annotations.Mapper;

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
     * @param loginDto
     * @return
     */
    String login(LoginDto loginDto);

    /**
     * 유저 id로 사용자 정보 조회
     * @param loginDto
     * @return
     */
    UserDto findByUserId(LoginDto loginDto);

    /**
     * 아이디 중복 체크
     * @param signUpDto
     * @return
     */
    Integer existsById(SignUpDto signUpDto);

    /**
     * 이메일 중복 체크
     * @param signUpDto
     * @return
     */
    Integer existsByEmail(SignUpDto signUpDto);

    /**
     * 회원가입
     * @param signUpDto
     */
    void signUpUser(SignUpDto signUpDto);
}
