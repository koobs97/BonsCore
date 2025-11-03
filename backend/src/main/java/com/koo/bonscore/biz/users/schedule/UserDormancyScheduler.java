package com.koo.bonscore.biz.users.schedule;

import com.koo.bonscore.biz.users.dto.DormantUserInfoDto;
import com.koo.bonscore.biz.users.service.UserService;
import com.koo.bonscore.common.api.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <pre>
 * UserDormancyScheduler.java
 * 설명 : 계정 휴면처리 관련 스케줄러
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDormancyScheduler {

    private final UserService userService;
    private final MailService mailService;

    /**
     * 매일 새벽 1시에 실행되어 30일 뒤 휴면 예정인 사용자에게 안내 메일을 발송.
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul") // 매일 새벽 1시
    // @Scheduled(cron = "0 */2 * * * *", zone = "Asia/Seoul") // 테스트용: 매 2분마다 실행
    public void sendDormancyPreNotification() {
        log.info("[SCHEDULE-START] 휴면 전환 '예정' 안내 작업을 시작합니다.");
        try {
            // 1. UserService를 통해 휴면 예정 대상자 조회 및 이메일 복호화
            List<DormantUserInfoDto> usersForNotice = userService.processDormancyNotice();

            // 2. 대상자가 없으면 작업 종료
            if (usersForNotice.isEmpty()) {
                log.info("[SCHEDULE-END] 휴면 전환 예정 대상자가 없어 작업을 종료합니다.");
                return;
            }

            // 3. 대상자들에게 안내 메일 발송
            log.info("[SCHEDULE-MAIL] {}명에게 휴면 전환 예정 안내 메일을 발송합니다.", usersForNotice.size());
            sendDormancyNoticeEmails(usersForNotice);

            log.info("[SCHEDULE-END] 휴면 전환 예정 안내 작업을 성공적으로 완료했습니다.");

        } catch (Exception e) {
            log.error("[SCHEDULE-ERROR] 휴면 전환 예정 안내 작업 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 휴면 전환 예정 사용자 목록을 받아 이메일을 발송하는 헬퍼 메소드
     * @param users 이메일을 발송할 사용자 목록
     */
    private void sendDormancyNoticeEmails(List<DormantUserInfoDto> users) {
        for (DormantUserInfoDto user : users) {
            try {
                mailService.sendDormancyNoticeEmail(user.getEmail(), user.getUserName());
            } catch (Exception e) {
                log.error("[SCHEDULE-MAIL-ERROR] 사용자 {}에게 휴면 예정 안내 메일 발송 중 오류 발생: {}", user.getUserId(), e.getMessage());
            }
        }
    }

    /**
     * 매일 새벽 2시에 실행되어 휴면 계정 처리 작업을 수행.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul") // 한국 시간 기준 매일 새벽 2시
    // @Scheduled(cron = "0 */2 * * * *", zone = "Asia/Seoul") // 테스트용: 매 2분마다 실행
    public void convertToDormantAccounts() {
        log.info("[SCHEDULE-START] 휴면 계정 전환 작업을 시작합니다.");

        try {
            // 1. UserService를 호출하여 핵심적인 DB 작업을 위임
            List<DormantUserInfoDto> convertedUsers = userService.processDormancy();

            // 2. 처리된 사용자가 없으면 작업 종료
            if (convertedUsers.isEmpty()) {
                log.info("[SCHEDULE-END] 휴면으로 전환할 대상이 없어 작업을 종료합니다.");
                return;
            }

            // 3. 처리된 사용자들에게 휴면 전환 완료 안내 메일 발송
            log.info("[SCHEDULE-MAIL] {}명에게 휴면 전환 완료 안내 메일을 발송합니다.", convertedUsers.size());
            sendDormancyCompletionEmails(convertedUsers);

            log.info("[SCHEDULE-END] 휴면 계정 전환 작업을 성공적으로 완료했습니다.");

        } catch (Exception e) {
            log.error("[SCHEDULE-ERROR] 휴면 계정 전환 작업 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 휴면 전환이 완료된 사용자 목록을 받아 이메일을 발송하는 헬퍼 메소드
     * @param users 이메일을 발송할 사용자 목록
     */
    private void sendDormancyCompletionEmails(List<DormantUserInfoDto> users) {
        for (DormantUserInfoDto user : users) {
            try {
                // 분리 보관 전의 이메일 주소로 발송
                mailService.sendDormancyCompleteEmail(user.getEmail(), user.getUserName());
            } catch (Exception e) {
                log.error("[SCHEDULE-MAIL-ERROR] 사용자 {}에게 휴면 전환 완료 메일 발송 중 오류 발생: {}", user.getUserId(), e.getMessage());
            }
        }
    }
}
