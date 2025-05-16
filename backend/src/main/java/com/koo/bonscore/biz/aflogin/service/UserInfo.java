package com.koo.bonscore.biz.aflogin.service;

import com.koo.bonscore.biz.aflogin.dto.req.UserReqIdDto;
import com.koo.bonscore.biz.aflogin.dto.res.UserInfoDto;
import com.koo.bonscore.biz.aflogin.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserInfo {

    private final UserInfoMapper userInfoMapper;

    public UserInfoDto getUserInfo(UserReqIdDto request) {
        return userInfoMapper.getUserInfo(request.getUserId());
    }

}
