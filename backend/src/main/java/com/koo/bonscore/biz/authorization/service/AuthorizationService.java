package com.koo.bonscore.biz.authorization.service;

import com.koo.bonscore.biz.auth.controller.RSAController;
import com.koo.bonscore.biz.auth.dto.req.SignUpDto;
import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.LogReqDto;
import com.koo.bonscore.biz.authorization.dto.req.UpdateUserDto;
import com.koo.bonscore.biz.authorization.dto.req.UserReqDto;
import com.koo.bonscore.biz.authorization.dto.res.*;
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

        UserReqDto input = UserReqDto.builder()
                .userId(request.getUserId())
                .email(request.getEmail().isEmpty() ? "" : encryptionService.hashWithSalt(request.getEmail()))
                .accountLocked(request.getAccountLocked())
                .withdrawn(request.getWithdrawn())
                .build();

        return authorizationMapper.getUserInfos(input)
                .stream()
                .peek(item -> {
                    item.setUserName(encryptionService.decrypt(item.getUserName()));
                    item.setEmail(encryptionService.decrypt(item.getEmail()));
                    item.setPhoneNumber(encryptionService.decrypt(item.getPhoneNumber()));
                })
                .filter(item ->
                        StringUtils.isEmpty(request.getUserName()) || request.getUserName().equals(item.getUserName())
                )
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
                .activityTypeList(authorizationMapper.getActivityType())
                .activityResultList(authorizationMapper.getActivityResult())
                .build();
    }

    /**
     * 로그 조회 화면의 로그
     * @param request LogReqDto
     * @return List<LogResDto>
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional
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
     * 보안질문 리스트 조회
     * @return 보안질문 리스트
     */
    public List<SecurityQuestionDto> getSecurityQuestion() {
        return authorizationMapper.getSecurityQuestion();
    }

    /**
     * 비밀번호 질문 및 답변 입력
     *
     * @param request 비밀번호 질문 및 답변 정보
     */
    @Transactional
    public void updateHintWithAns(UpdateUserDto request) {

        UpdateUserDto item = UpdateUserDto.builder()
                .userId(request.getUserId())
                .passwordHint(request.getPasswordHint())
                .passwordHintAnswer(encryptionService.encrypt(request.getPasswordHintAnswer()))
                .updatedAt(LocalDateTime.now())
                .build();

        authorizationMapper.updateHintWithAns(item);
    }

    /**
     * 회원탈퇴
     * 개인 식별 정보(PII)는 즉시 NULL 값이나 빈 문자열로 변경하여 식별이 불가능하도록 처리
     * @param userId 유저 ID
     */
    @Transactional
    public void updateWithdrawn(String userId) {

        UpdateUserDto item = UpdateUserDto.builder()
                .userId(userId)
                .updatedAt(LocalDateTime.now())
                .build();

        authorizationMapper.updateWithdrawn(item);
    }
}
