package com.koo.bonscore.log.service;

import com.koo.bonscore.log.dto.UserActivityLogDto;
import com.koo.bonscore.log.mapper.UserActivityLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <pre>
 * UserActivityLogService.java
 * 설명 : 사용자 활동 로그 저장 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-07-23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityLogService {

    private final UserActivityLogMapper userActivityLogMapper;

    /**
     * 사용자 활동 로그를 DB에 저장합니다.
     *
     * @Async: 이 메소드는 별도의 스레드(logTaskExecutor)에서 비동기적으로 실행됩니다.
     * 호출 스레드는 이 메소드가 끝날 때까지 기다리지 않습니다.
     * @Transactional(propagation = Propagation.REQUIRES_NEW):
     * 이 메소드는 항상 새로운 트랜잭션에서 실행됩니다.
     * 원래의 비즈니스 로직(로그인 등)의 트랜잭션과 분리되어
     * 로그 저장 실패가 원래 로직의 롤백에 영향을 주지 않습니다.
     */
    @Async("logTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(UserActivityLogDto logDto) {
        try {
            userActivityLogMapper.insertUserActivityLog(logDto);
        } catch (Exception e) {
            log.error("사용자 활동 로그 저장 실패. Log DTO: {}", logDto, e);
        }
    }
}