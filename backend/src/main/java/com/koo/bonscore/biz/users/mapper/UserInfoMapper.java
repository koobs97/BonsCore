package com.koo.bonscore.biz.users.mapper;

import com.koo.bonscore.biz.users.dto.res.UserInfoDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserInfoMapper {
    UserInfoDto getUserInfo(String userId);
    UserInfoDto getUserRole(String userId);
}
