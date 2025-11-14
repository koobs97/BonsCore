package com.koo.bonscore.biz.users.service;

import com.koo.bonscore.biz.auth.dto.req.UserInfoSearchDto;
import com.koo.bonscore.biz.auth.mapper.AuthMapper;
import com.koo.bonscore.biz.users.dto.DormantUserInfoDto;
import com.koo.bonscore.biz.users.dto.res.UserInfoDto;
import com.koo.bonscore.biz.users.mapper.UserInfoMapper;
import com.koo.bonscore.core.config.enc.EncryptionService;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    private final UserInfoMapper userInfoMapper;
    private final EncryptionService encryptionService;

    // 인증 mapper
    private final AuthMapper authMapper;

    /**
     * 사용자 정보 가져오기
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    @Transactional(readOnly = true)
    public UserInfoDto getUserInfo(String userId) {
        UserInfoDto infoDto = userInfoMapper.getUserInfo(userId);
        UserInfoDto roleDto = userInfoMapper.getUserRole(userId);
        if(roleDto == null) {
            throw new BsCoreException(HttpStatusCode.FORBIDDEN, ErrorCode.UNAUTHORIZED);
        }

        return UserInfoDto.builder()
                .userId(userId)
                .userName(encryptionService.decrypt(infoDto.getUserName()))
                .email(encryptionService.decrypt(infoDto.getEmail()))
                .birthDate(encryptionService.decrypt(infoDto.getBirthDate()))
                .phoneNumber(encryptionService.decrypt(infoDto.getPhoneNumber()))
                .genderCode(infoDto.getGenderCode())
                .loginTime(String.valueOf(LocalDateTime.now()))
                .roleId(roleDto.getRoleId())
                .build();
    }

    /**
     * 휴면 전환 예정인 사용자 목록을 조회
     * @return 안내 메일 발송 대상 사용자 목록
     */
    @Transactional(readOnly = true)
    public List<DormantUserInfoDto> processDormancyNotice() {
        // 1. 30일 뒤 휴면 전환 예정자 조회
        List<DormantUserInfoDto> usersForNotice = userInfoMapper.getUsersForDormancyNotice();

        if (usersForNotice == null || usersForNotice.isEmpty()) {
            return List.of();
        }

        log.info("총 {}명의 사용자에게 휴면 전환 예정 안내를 처리합니다.", usersForNotice.size());

        // 2. 메일 발송을 위해 이메일 주소 복호화
        usersForNotice.forEach(user -> {
            try {
                user.setUserName(encryptionService.decrypt(user.getUserName()));
                user.setEmail(encryptionService.decrypt(user.getEmail()));
            } catch (Exception e) {
                log.error("휴면 예정 안내 대상자 이메일 복호화 실패: userId={}, email={}", user.getUserId(), user.getEmail(), e);
            }
        });

        return usersForNotice;
    }

    /**
     * 휴면 전환 대상자 사용자 목록을 조회
     * @return 안내 메일 발송 대상 사용자 목록
     */
    @Transactional
    public List<DormantUserInfoDto> processDormancy() {
        // 1. 휴면 전환 대상자 조회
        List<DormantUserInfoDto> usersToConvert = userInfoMapper.getDormantUsers();

        if (usersToConvert == null || usersToConvert.isEmpty()) {
            return List.of();
        }

        log.info("총 {}명의 사용자를 휴면 계정으로 전환합니다.", usersToConvert.size());

        // 2. 개인정보를 분리 보관 테이블에 삽입
        userInfoMapper.insertDormantUsers(usersToConvert);

        // 3. 원본 테이블의 개인정보 파기 및 상태 업데이트
        List<String> userIds = usersToConvert.stream()
                .map(DormantUserInfoDto::getUserId)
                .collect(Collectors.toList());
        userInfoMapper.updateUsersToDormantState(userIds);

        log.info("사용자 정보 분리 보관 및 원본 테이블 업데이트 완료.");

        usersToConvert.forEach(user -> {
            try {
                user.setUserName(encryptionService.decrypt(user.getUserName()));
                user.setEmail(encryptionService.decrypt(user.getEmail()));
            } catch (Exception e) {
                // 암호화 실패 시 예외 처리
                log.error("이메일 암호화 실패: userId={}, email={}", user.getUserId(), user.getEmail(), e);
            }
        });

        return usersToConvert;
    }

    /**
     * 휴면 계정을 활성화(원복) 한다.
     *
     * @param request 인증에 사용된 이메일 정보
     */
    @Transactional
    public void activateDormantUser(DormantUserInfoDto request) {

        // 유저 조회
        UserInfoSearchDto input = UserInfoSearchDto.builder()
                .email(encryptionService.hashWithSalt(request.getEmail()))
                .updatedAt(LocalDateTime.now())
                .build();

        // 이메일과 일치하는 정보 조회
        UserInfoSearchDto result = authMapper.findDormantUserByNameAndEmail(input);
        String userId = result.getUserId();

        // 1. 휴면 테이블에서 분리 보관된 사용자 정보를 조회합니다.
        DormantUserInfoDto dormantUserInfo = userInfoMapper.getDormantUserInfo(userId);

        if (dormantUserInfo == null) {
            log.warn("휴면 해제 요청이 있었으나, 휴면 정보 테이블에 해당 사용자가 없습니다. UserId: {}", userId);
            throw new BsCoreException(ErrorCode.DORMANT_USER_NOT_FOUND);
        }

        // 원본 유저 테이블 복원
        userInfoMapper.restoreUserFromDormancy(dormantUserInfo);

        // 휴면 정보 테이블 사용자 정보 삭제
        userInfoMapper.deleteDormantUserInfo(userId);

        log.info("사용자 계정이 성공적으로 활성화되었습니다. UserId: {}", userId);
    }

}
