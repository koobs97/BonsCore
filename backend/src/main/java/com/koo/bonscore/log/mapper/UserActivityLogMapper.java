package com.koo.bonscore.log.mapper;

import com.koo.bonscore.log.dto.UserActivityLogDto;
import org.apache.ibatis.annotations.Mapper;

/**
 * <pre>
 * UserActivityLogMapper.java
 * 설명 : 사용자 활동 로그 접근을 처리하는 MyBatis 매퍼 인터페이스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-07-23
 */
@Mapper
public interface UserActivityLogMapper {

    /**
     * 사용자 활동 로그 정보 저장
     * @param logDto
     */
    void insertUserActivityLog(UserActivityLogDto logDto);
}
