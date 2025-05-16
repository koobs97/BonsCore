package com.koo.bonscore.biz.aflogin.mapper;

import com.koo.bonscore.biz.aflogin.dto.res.UserInfoDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserInfoMapper {
    UserInfoDto getUserInfo(String userId);
}
