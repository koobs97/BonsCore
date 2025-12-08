package com.koo.bonscore.biz.users.service;

import com.koo.bonscore.biz.auth.entity.User;
import com.koo.bonscore.biz.auth.service.AuthService;
import com.koo.bonscore.biz.users.entity.UserDormantInfo;
import com.koo.bonscore.biz.auth.repository.UserRepository;
import com.koo.bonscore.biz.users.repository.UserDormantRepository;
import com.koo.bonscore.biz.users.dto.DormantUserInfoDto;
import com.koo.bonscore.biz.users.dto.res.UserInfoDto;
import com.koo.bonscore.core.config.enc.EncryptionService;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserDormantRepository userDormantRepository;

    private final EncryptionService encryptionService;
    private final AuthService authService;

    /**
     * 사용자 정보 가져오기
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    @Transactional(readOnly = true)
    public UserInfoDto getUserInfo(String userId) {

        // 1. 유저 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BsCoreException(HttpStatusCode.NOT_FOUND, ErrorCode.INVALID_CREDENTIALS));

        // 2. 권한 조회 (UserRepository의 native query 활용)
        List<String> roles = authService.getRoles(userId);
        if(roles.isEmpty()) {
            throw new BsCoreException(HttpStatusCode.FORBIDDEN, ErrorCode.UNAUTHORIZED);
        }
        String mainRole = roles.get(0);

        return UserInfoDto.builder()
                .userId(user.getUserId())
                .userName(encryptionService.decrypt(user.getUserName()))
                .email(encryptionService.decrypt(user.getEmail()))
                .birthDate(encryptionService.decrypt(user.getBirthDate()))
                .phoneNumber(encryptionService.decrypt(user.getPhoneNumber()))
                .genderCode(user.getGenderCode())
                .loginTime(String.valueOf(LocalDateTime.now()))
                .roleId(mainRole)
                .build();
    }

    /**
     * 휴면 전환 예정인 사용자 목록을 조회
     * @return 안내 메일 발송 대상 사용자 목록
     */
    @Transactional(readOnly = true)
    public List<DormantUserInfoDto> processDormancyNotice() {
        // 기준일 설정 (예: 11개월 전)
        LocalDateTime thresholdDate = LocalDateTime.now().minusMonths(11);

        // 1. 30일 뒤 휴면 전환 예정자 조회 (마지막 로그인이 11개월 전이고, 현재 활성 상태인 유저)
        List<User> users = userRepository.findByLastLoginAtBeforeAndAccountLocked(thresholdDate, "N");

        if (users.isEmpty()) {
            return List.of();
        }

        log.info("총 {}명의 사용자에게 휴면 전환 예정 안내를 처리합니다.", users.size());

        List<DormantUserInfoDto> noticeList = new ArrayList<>();

        // 2. 메일 발송을 위해 이메일 주소 복호화
        for (User user : users) {
            try {
                DormantUserInfoDto dto = DormantUserInfoDto.builder()
                        .userId(user.getUserId())
                        .userName(encryptionService.decrypt(user.getUserName()))
                        .email(encryptionService.decrypt(user.getEmail()))
                        .lastLoginAt(user.getLastLoginAt())
                        .build();
                noticeList.add(dto);
            } catch (Exception e) {
                log.error("휴면 예정 안내 대상자 복호화 실패: userId={}", user.getUserId(), e);
            }
        }

        return noticeList;
    }

    /**
     * 휴면 전환 대상자 사용자 목록을 조회
     * @return 안내 메일 발송 대상 사용자 목록
     */
    @Transactional
    public List<DormantUserInfoDto> processDormancy() {

        // 기준일 설정 (예: 1년 전)
        LocalDateTime thresholdDate = LocalDateTime.now().minusYears(1);

        // 1. 휴면 전환 대상자 조회
        List<User> usersToConvert = userRepository.findByLastLoginAtBeforeAndAccountLocked(thresholdDate, "N");

        if (usersToConvert == null || usersToConvert.isEmpty()) {
            return List.of();
        }

        log.info("총 {}명의 사용자를 휴면 계정으로 전환합니다.", usersToConvert.size());

        // 2. 개인정보를 분리 보관 테이블에 삽입
        List<DormantUserInfoDto> convertedUsers = new ArrayList<>();

        for (User user : usersToConvert) {
            try {
                // 2. 휴면 정보 테이블(UserDormantInfo)에 보관 (Entity 생성)
                UserDormantInfo dormantInfo = UserDormantInfo.builder()
                        .userId(user.getUserId())
                        .userName(user.getUserName()) // 암호화된 상태 그대로 저장
                        .email(user.getEmail())
                        .emailHash(user.getEmailHash())
                        .phoneNumber(user.getPhoneNumber())
                        .birthDate(user.getBirthDate())
                        .genderCode(user.getGenderCode())
                        // 추가 컬럼 매핑
                        .passwordHint(user.getPasswordHint())
                        .passwordHintAnswer(user.getPasswordHintAnswer())
                        .passwordUpdated(user.getPasswordUpdated())
                        .termsAgree1(user.getTermsAgree1())
                        .termsAgree2(user.getTermsAgree2())
                        .termsAgree3(user.getTermsAgree3())
                        .termsAgree4(user.getTermsAgree4())
                        .createdAt(LocalDateTime.now())
                        .build();

                userDormantRepository.save(dormantInfo);

                // 3. 원본 User 테이블 상태 업데이트 (Dirty Checking 활용)
                // -> 개인정보 파기 및 accountLocked = 'Y' 설정
                user.convertToDormant();

                // 4. 결과 리스트에 추가 (메일 발송 등을 위함 - 복호화)
                DormantUserInfoDto resultDto = DormantUserInfoDto.builder()
                        .userId(user.getUserId()) // convertToDormant 호출 전에 ID 등은 유지됨
                        .userName(encryptionService.decrypt(dormantInfo.getUserName()))
                        .email(encryptionService.decrypt(dormantInfo.getEmail()))
                        .build();

                convertedUsers.add(resultDto);

            } catch (Exception e) {
                log.error("휴면 전환 처리 중 오류 발생: userId={}", user.getUserId(), e);
            }
        }

        log.info("사용자 정보 분리 보관 및 원본 테이블 업데이트 완료.");
        return convertedUsers;
    }

    /**
     * 휴면 계정을 활성화(원복) 한다.
     *
     * @param request 인증에 사용된 이메일 정보
     */
    @Transactional
    public void activateDormantUser(DormantUserInfoDto request) {

        String emailHash = encryptionService.hashWithSalt(request.getEmail());

        // 1. 휴면 테이블에서 사용자 정보 조회 (이메일 Hash 이용)
        UserDormantInfo dormantUserInfo = userDormantRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> {
                    log.warn("휴면 해제 요청 실패 - 휴면 정보 없음. EmailHash: {}", emailHash);
                    return new BsCoreException(ErrorCode.DORMANT_USER_NOT_FOUND);
                });

        String userId = dormantUserInfo.getUserId();

        // 2. 원본 유저 테이블 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BsCoreException(ErrorCode.INVALID_CREDENTIALS));

        // 3. 원본 유저 테이블 복원 (User Entity의 restore 메서드 활용)
        // -> UserDormantInfo의 데이터를 User로 이동
        user.restoreFromDormancy(dormantUserInfo);

        // 4. 휴면 정보 테이블 데이터 삭제
        userDormantRepository.delete(dormantUserInfo);

        log.info("사용자 계정이 성공적으로 활성화되었습니다. UserId: {}", userId);
    }

}
