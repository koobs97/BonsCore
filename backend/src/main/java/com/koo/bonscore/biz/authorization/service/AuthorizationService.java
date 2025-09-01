package com.koo.bonscore.biz.authorization.service;

import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.LogReqDto;
import com.koo.bonscore.biz.authorization.dto.req.UpdateUserDto;
import com.koo.bonscore.biz.authorization.dto.res.ActivityResponseDto;
import com.koo.bonscore.biz.authorization.dto.res.LogResDto;
import com.koo.bonscore.biz.authorization.dto.res.MenuByRoleDto;
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
    private final EncryptionService encryptionService;
    private final BCryptPasswordEncoder passwordEncoder; // SecurityConfig에서 bean 생성

    /**
     * 권한에 맞는 메뉴 조회
     * @param request AuthorizationDto
     * @return List<MenuByRoleDto>
     */
    @Transactional
    public List<MenuByRoleDto> getMenuByRole(AuthorizationDto request) {
        return authorizationMapper.getMenuByRole(request);
    }

    /**
     * 검색조건에 쓰일 코드 조회
     *
     * @param request LogReqDto
     * @return ActivityResponseDto
     */
    @Transactional
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
}
