package com.koo.bonscore.biz.users.service;

import com.koo.bonscore.biz.users.dto.req.UserReqIdDto;
import com.koo.bonscore.biz.users.dto.res.UserInfoDto;
import com.koo.bonscore.biz.users.mapper.UserInfoMapper;
import com.koo.bonscore.core.config.enc.EncryptionService;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <pre>
 * UserService.java
 * 설명 : 사용자 정보 관리 - "인증된 사용자의 정보 조회 및 수정"
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-16
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserInfoMapper userInfoMapper;
    private final EncryptionService encryptionService;

    /**
     * 유저정보 가져오기
     * @param request
     * @return
     */
    public UserInfoDto getUserInfo(UserReqIdDto request) {
        UserInfoDto infoDto = userInfoMapper.getUserInfo(request.getUserId());
        UserInfoDto roleDto = userInfoMapper.getUserRole(request.getUserId());
        if(roleDto == null) {
            throw new BsCoreException(HttpStatusCode.FORBIDDEN, ErrorCode.UNAUTHORIZED);
        }

        return UserInfoDto.builder()
                .userId(request.getUserId())
                .userName(encryptionService.decrypt(infoDto.getUserName()))
                .email(encryptionService.decrypt(infoDto.getEmail()))
                .birthDate(encryptionService.decrypt(infoDto.getBirthDate()))
                .phoneNumber(encryptionService.decrypt(infoDto.getPhoneNumber()))
                .genderCode(infoDto.getGenderCode())
                .loginTime(String.valueOf(LocalDateTime.now()))
                .roleId(roleDto.getRoleId())
                .build();
    }

}
