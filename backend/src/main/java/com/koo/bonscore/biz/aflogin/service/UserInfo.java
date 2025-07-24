package com.koo.bonscore.biz.aflogin.service;

import com.koo.bonscore.biz.aflogin.dto.req.UserReqIdDto;
import com.koo.bonscore.biz.aflogin.dto.res.UserInfoDto;
import com.koo.bonscore.biz.aflogin.mapper.UserInfoMapper;
import com.koo.bonscore.core.config.enc.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <pre>
 * AuthService.java
 * 설명 : 로그인 이후 서비스(유저정보 조회 등)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-16
 */
@Service
@RequiredArgsConstructor
public class UserInfo {

    private final UserInfoMapper userInfoMapper;
    private final EncryptionService encryptionService;

    /**
     * 유저정보 가져오기
     * @param request
     * @return
     */
    public UserInfoDto getUserInfo(UserReqIdDto request) {
        UserInfoDto dto = userInfoMapper.getUserInfo(request.getUserId());
        dto.setUserName(encryptionService.decrypt(dto.getUserName()));
        dto.setEmail(encryptionService.decrypt(dto.getEmail()));
        dto.setBirthDate(encryptionService.decrypt(dto.getBirthDate()));
        dto.setPhoneNumber(encryptionService.decrypt(dto.getPhoneNumber()));
        dto.setLoginTime(String.valueOf(LocalDateTime.now()));
        return dto;
    }

}
