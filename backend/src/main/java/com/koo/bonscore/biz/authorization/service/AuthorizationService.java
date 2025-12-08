package com.koo.bonscore.biz.authorization.service;

import com.koo.bonscore.biz.auth.controller.RSAController;
import com.koo.bonscore.biz.auth.entity.User;
import com.koo.bonscore.biz.auth.repository.UserRepository;
import com.koo.bonscore.biz.authorization.dto.req.AuthorizationDto;
import com.koo.bonscore.biz.authorization.dto.req.LogReqDto;
import com.koo.bonscore.biz.authorization.dto.req.UpdateUserDto;
import com.koo.bonscore.biz.authorization.dto.req.UserReqDto;
import com.koo.bonscore.biz.authorization.dto.res.*;
import com.koo.bonscore.biz.authorization.entity.Menu;
import com.koo.bonscore.biz.authorization.entity.Role;
import com.koo.bonscore.biz.authorization.repository.SecurityQuestionRepository;
import com.koo.bonscore.log.repository.UserActivityLogRepository;
import com.koo.bonscore.core.config.enc.EncryptionService;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.log.entity.UserActivityLog;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    private final UserRepository userRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final SecurityQuestionRepository securityQuestionRepository;

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

        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new BsCoreException(ErrorCode.USER_INFO_NOT_MATCH));

        List<MenuByRoleDto> result = new ArrayList<>();

        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                for (Menu menu : role.getMenus()) {
                    // 3. Entity -> DTO 변환
                    result.add(MenuByRoleDto.builder()
                            .menuId(menu.getMenuId())
                            .menuName(menu.getMenuName())
                            .menuUrl(menu.getMenuUrl())
                            .parentMenuId(menu.getParentMenuId())
                            .sortOrder(menu.getSortOrder())
                            .isVisible(menu.getIsVisible())
                            .build());
                }
            }
        }

        // 중복 제거 및 정렬이 필요하다면 Java Stream으로 추가 처리
        return result;
    }

    /**
     * 사용자 관리 정보 조회
     * @param request 검색조건
     * @return List<UserResDto> 사용자 정보 리스트
     */
    @Transactional(readOnly = true)
    public List<UserResDto> getUserInfos(UserReqDto request) {

        String userId = StringUtils.isEmpty(request.getUserId()) ? null : request.getUserId();
        String emailHash = StringUtils.isEmpty(request.getEmail()) ? null : encryptionService.hashWithSalt(request.getEmail());
        String accountLocked = StringUtils.isEmpty(request.getAccountLocked()) ? null : request.getAccountLocked();
        String withdrawn = StringUtils.isEmpty(request.getWithdrawn()) ? null : request.getWithdrawn();

        // 2. Repository 호출 (@Query 메서드)
        List<User> users = userRepository.findUsersByCondition(userId, emailHash, accountLocked, withdrawn);

        // 3. Entity -> DTO 변환 및 복호화
        return users.stream()
                .map(user -> {
                    UserResDto dto = UserResDto.builder()
                            .userId(user.getUserId())
                            .userName(user.getUserName())
                            .email(user.getEmail())
                            .phoneNumber(user.getPhoneNumber())
                            .accountLocked(user.getAccountLocked())
                            .withdrawn(user.getWithdrawn())
                            .lastLoginAt(user.getLastLoginAt())
                            .build();

                    // 복호화 처리
                    if ("DORMANT_USER".equals(dto.getUserName())) {
                        dto.setUserName("DORMANT_USER");
                    } else if (dto.getUserName() != null) {
                        try {
                            dto.setUserName(encryptionService.decrypt(dto.getUserName()));
                        } catch (Exception e) { /* 복호화 실패 시 원본 유지 */ }
                    }
                    if (dto.getEmail() != null) dto.setEmail(encryptionService.decrypt(dto.getEmail()));
                    if (dto.getPhoneNumber() != null) dto.setPhoneNumber(encryptionService.decrypt(dto.getPhoneNumber()));

                    return dto;
                })
                // 이름 검색 필터링 (메모리상 수행)
                .filter(item -> StringUtils.isEmpty(request.getUserName()) || request.getUserName().equals(item.getUserName()))
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
                .activityTypeList(userActivityLogRepository.findDistinctActivityTypes())
                .activityResultList(userActivityLogRepository.findDistinctActivityResults())
                .build();
    }

    /**
     * 로그 조회 화면의 로그
     * @param request LogReqDto
     * @return List<LogResDto>
     */
    @Transactional(readOnly = true)
    public List<LogResDto> getUserLog(LogReqDto request) {

        // 날짜 기본값 설정
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        if (StringUtils.isEmpty(request.getStartDt())) {
            request.setStartDt(LocalDate.now().withMonth(1).withDayOfMonth(1).format(formatter));
        }
        if (StringUtils.isEmpty(request.getEndDt())) {
            request.setEndDt(LocalDate.now().format(formatter));
        }

        // String(yyyyMMdd) -> LocalDateTime 변환
        LocalDateTime startDateTime = LocalDate.parse(request.getStartDt(), formatter).atStartOfDay();
        LocalDateTime endDateTime = LocalDate.parse(request.getEndDt(), formatter).atTime(LocalTime.MAX);

        // 동적 쿼리 생성
        Specification<UserActivityLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 날짜 범위 (createdAt 기준 가정)
            predicates.add(cb.between(root.get("createdAt"), startDateTime, endDateTime));

            if (StringUtils.isNotEmpty(request.getUserId())) {
                predicates.add(cb.like(root.get("userId"), "%" + request.getUserId() + "%"));
            }
            if (StringUtils.isNotEmpty(request.getActivityType())) {
                predicates.add(cb.equal(root.get("activityType"), request.getActivityType()));
            }
            if (StringUtils.isNotEmpty(request.getActivityResult())) {
                predicates.add(cb.equal(root.get("activityResult"), request.getActivityResult()));
            }

            // 정렬 (최신순)
            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 조회 및 변환
        return userActivityLogRepository.findAll(spec).stream()
                .map(log -> LogResDto.builder()
                        .logId(log.getLogId()) // ID 필드명 확인 필요
                        .userId(log.getUserId())
                        .activityType(log.getActivityType())
                        .activityResult(log.getActivityResult())
                        .requestIp(log.getRequestIp())
                        .createdAt(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 유저 정보 업데이트
     * @param request UpdateUserDto
     */
    @Transactional
    public void updateUserInfo(UpdateUserDto request) {

        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new BsCoreException(ErrorCode.USER_INFO_NOT_MATCH));

        // 데이터 암호화 및 해싱
        String encName = encryptionService.encrypt(request.getUserName());
        String encEmail = encryptionService.encrypt(request.getEmail());
        String hashEmail = encryptionService.hashWithSalt(request.getEmail());
        String encPhone = encryptionService.encrypt(request.getPhoneNumber());
        String encBirth = encryptionService.encrypt(request.getBirthDate());

        // Entity 메서드를 통해 업데이트 (JPA Dirty Checking)
        user.updateInfo(encName, encEmail, hashEmail, encPhone, encBirth, request.getGenderCode());
    }

    /**
     * 현재 비밀번호 확인
     * @param request UpdateUserDto
     * @return Boolean result
     * @throws Exception ex
     */
    @Transactional(readOnly = true)
    public boolean passwordValidate(UpdateUserDto request) throws Exception {

        // DB에서 사용자 조회
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new BsCoreException(ErrorCode.USER_INFO_NOT_MATCH));

        // 입력받은 비밀번호 복호화
        String decryptedInputPassword = rsaController.decrypt(request.getPassword());

        // matches(평문, 해시) 비교
        return passwordEncoder.matches(decryptedInputPassword, user.getPassword());
    }

    /**
     * 비밀번호 업데이트
     *
     * @param request UpdateUserDto
     * @throws Exception e
     */
    @Transactional
    public void updatePassword(UpdateUserDto request) throws Exception {

        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new BsCoreException(ErrorCode.USER_INFO_NOT_MATCH));

        String rawPassword = rsaController.decrypt(request.getPassword());
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Dirty Checking
        user.updatePassword(encodedPassword);
    }

    /**
     * 보안질문 리스트 조회
     * @return 보안질문 리스트
     */
    public List<SecurityQuestionDto> getSecurityQuestion() {
        return securityQuestionRepository.findAllByUseYnOrderByDisplayOrder("Y").stream()
                .map(q -> SecurityQuestionDto.builder()
                        .questionCode(q.getQuestionCode()) // 필드명 가정
                        .questionText(q.getQuestionText())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 비밀번호 질문 및 답변 입력
     *
     * @param request 비밀번호 질문 및 답변 정보
     */
    @Transactional
    public void updateHintWithAns(UpdateUserDto request) {

        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new BsCoreException(ErrorCode.USER_INFO_NOT_MATCH));

        String encAnswer = encryptionService.encrypt(request.getPasswordHintAnswer());

        user.updateSecurityHint(request.getPasswordHint(), encAnswer);
    }

    /**
     * 회원탈퇴
     * 개인 식별 정보(PII)는 즉시 NULL 값이나 빈 문자열로 변경하여 식별이 불가능하도록 처리
     * @param userId 유저 ID
     */
    @Transactional
    public void updateWithdrawn(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BsCoreException(ErrorCode.USER_INFO_NOT_MATCH));

        // Entity의 비즈니스 메서드 호출
        user.withdrawUser();
    }
}
