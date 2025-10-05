package com.koo.bonscore.biz.authorization.service;

import com.koo.bonscore.biz.auth.controller.RSAController;
import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.LogReqDto;
import com.koo.bonscore.biz.authorization.dto.req.UpdateUserDto;
import com.koo.bonscore.biz.authorization.dto.req.UserReqDto;
import com.koo.bonscore.biz.authorization.dto.res.ActivityResponseDto;
import com.koo.bonscore.biz.authorization.dto.res.LogResDto;
import com.koo.bonscore.biz.authorization.dto.res.MenuByRoleDto;
import com.koo.bonscore.biz.authorization.dto.res.UserResDto;
import com.koo.bonscore.biz.authorization.mapper.AuthorizationMapper;
import com.koo.bonscore.core.config.enc.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <pre>
 * AuthorizationService.java
 * 설명 : 인가 (Authorization) 관련 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-01
 */
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final AuthorizationMapper authorizationMapper;
    private final RSAController rsaController;
    private final EncryptionService encryptionService;
    private final BCryptPasswordEncoder passwordEncoder; // SecurityConfig에서 bean 생성

    /**
     * 권한에 맞는 메뉴 조회
     * @param request AuthorizationDto
     * @return List<MenuByRoleDto>
     */
    @Transactional(readOnly = true)
    public List<MenuByRoleDto> getMenuByRole(AuthorizationDto request) {
        return authorizationMapper.getMenuByRole(request);
    }

    /**
     * 사용자 관리 정보 조회
     * @param request 검색조건
     * @return List<UserResDto> 사용자 정보 리스트
     */
    @Transactional(readOnly = true)
    public List<UserResDto> getUserInfos(UserReqDto request) {
        return authorizationMapper.getUserInfos(request)
                .stream()
                .peek(item -> {
                    item.setUserName(encryptionService.decrypt(item.getUserName()));
                    item.setEmail(encryptionService.decrypt(item.getEmail()));
                    item.setPhoneNumber(encryptionService.decrypt(item.getPhoneNumber()));
                })
                .collect(Collectors.toList());
    }

    /**
     * 검색조건에 쓰일 코드 조회
     *
     * @param request LogReqDto
     * @return ActivityResponseDto
     */
    @Transactional(readOnly = true)
    public ActivityResponseDto getActivityCd(LogReqDto request) {
        return ActivityResponseDto.builder()
                .activityTypeList(authorizationMapper.getActivityType(request))
                .activityResultList(authorizationMapper.getActivityResult(request))
                .build();
    }

    /**
     * 로그 조회 화면의 로그
     * @param request LogReqDto
     * @return List<LogResDto>
     */
    @Transactional
    public List<LogResDto> getUserLog(LogReqDto request) {

        if(StringUtils.isEmpty(request.getStartDt())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            request.setStartDt(LocalDate.now()
                    .withMonth(1)
                    .withDayOfMonth(1)
                    .format(formatter));
        }
        if(StringUtils.isEmpty(request.getEndDt())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            request.setEndDt(LocalDate.now().format(formatter));
        }

        return authorizationMapper.getUserLog(request);
    }

    /**
     * 유저 정보 업데이트
     * @param request UpdateUserDto
     */
    @Transactional
    public void updateUserInfo(UpdateUserDto request) {

        // 암호화 대상 : 유저명, 패스워드, 이메일, 전화번호, 생년월일
        UpdateUserDto item = UpdateUserDto.builder()
                .userId(request.getUserId())
                .userName(encryptionService.encrypt(request.getUserName()))
                .email(encryptionService.encrypt(request.getEmail()))
                .emailHash(encryptionService.hashWithSalt(request.getEmail()))
                .phoneNumber(encryptionService.encrypt(request.getPhoneNumber()))
                .birthDate(encryptionService.encrypt(request.getBirthDate()))
                .genderCode(request.getGenderCode())
                .updatedAt(LocalDateTime.now())
                .build();

        authorizationMapper.updateUserInfo(item);
    }

    /**
     * 현재 비밀번호 확인
     * @param request UpdateUserDto
     * @return Boolean result
     * @throws Exception ex
     */
    public boolean passwordValidate(UpdateUserDto request) throws Exception {

        // 받아온 password
        String decryptedPassword = rsaController.decrypt(request.getPassword());

        // userId의 해싱된 passwd get
        String getHasedPassword = authorizationMapper.getPassword(request);

        // 비밀번호 비교는 matches 함수 사용
        return passwordEncoder.matches(decryptedPassword, getHasedPassword);
    }

    /**
     * 비밀번호 업데이트
     *
     * @param request UpdateUserDto
     * @throws Exception e
     */
    public void updatePassword(UpdateUserDto request) throws Exception {

        UpdateUserDto item = UpdateUserDto.builder()
                .userId(request.getUserId())
                .password(passwordEncoder.encode(rsaController.decrypt(request.getPassword())))
                .passwordUpdated(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .updatedAt(LocalDateTime.now())
                .build();
        
        authorizationMapper.updatePassword(item);
    }

    /**
     * 회원탈퇴
     * 개인 식별 정보(PII)는 즉시 NULL 값이나 빈 문자열로 변경하여 식별이 불가능하도록 처리
     * @param userId 유저 id
     * @throws Exception e
     */
    public void updateWithdrawn(String userId) throws Exception {

        UpdateUserDto item = UpdateUserDto.builder()
                .userId(userId)
                .updatedAt(LocalDateTime.now())
                .build();

        authorizationMapper.updateWithdrawn(item);
    }
}
