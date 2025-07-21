package com.koo.bonscore.biz.auth.mapper;

import com.koo.bonscore.biz.auth.dto.UserDto;
import com.koo.bonscore.biz.auth.dto.req.LoginDto;
import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthMapper {
    String login(LoginDto loginDto);
    UserDto findByUserId(LoginDto loginDto);
    Integer existsById(SignUpDto signUpDto);
}
