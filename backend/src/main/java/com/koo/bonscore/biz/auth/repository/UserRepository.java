package com.koo.bonscore.biz.auth.repository;

import com.koo.bonscore.biz.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <pre>
 * UserRepository.java
 * 설명 : User repository
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 사용자 ID로 회원정보 조회
     * @param userId 사용자 ID
     * @return 존재할 경우 User 객체를 포함한 Optional, 없으면 빈 Optional
     */
    Optional<User> findByUserId(String userId);

    /**
     * 해당 사용자 ID가 이미 존재하는지 확인(회원가입 중복 체크)
     * @param userId 확인할 사용자 ID
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByUserId(String userId);

    /**
     * 해당 이메일 해시값이 이미 존재하는지 확인(회원가입 중복 체크)
     * @param emailHash 확인할 이메일 해시값
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByEmailHash(String emailHash);

    /**
     * 이메일 해시값으로 회원 정보를 조회
     * @param emailHash 조회할 이메일 해시
     * @return User 객체 (Optional)
     */
    Optional<User> findByEmailHash(String emailHash);

    /**
     * 이메일 해시와 이름으로 조회 (아이디 찾기 등)
     * @param emailHash 이메일 해시
     * @param userName 사용자명
     * @return 조건에 맞는 User 객체 (Optional)
     */
    Optional<User> findByEmailHashAndUserName(String emailHash, String userName);

    // 휴면 대상자 조회 (예: 마지막 로그인 시간이 thresholdDate 이전이고, 아직 휴면 상태가 아닌(N) 유저)

    /**
     * 휴면 대상자 조회
     * <p>
     * 조건 1: 마지막 로그인 시간이 기준일(thresholdDate)보다 이전(Before)이어야 함
     * 조건 2: 현재 계정 잠금 상태(accountLocked)가 특정 값(예: 'N')이어야 함
     * </p>
     *
     * @param thresholdDate 휴면 전환 기준 날짜 (예: 1년 전)
     * @param accountLocked 계정 잠금 여부 필터 (보통 'N'인 사람을 찾음)
     * @return 휴면 전환 대상 User 리스트
     */
    List<User> findByLastLoginAtBeforeAndAccountLocked(LocalDateTime thresholdDate, String accountLocked);

    /**
     * 소셜 로그인(OAuth) 정보로 회원을 조회
     * @param oauthProvider oauthProvider 제공자 (예: google, kakao)
     * @param oauthProviderId oauthProviderId 제공자 측 식별 ID
     * @return 연동된 User 객체 (Optional)
     */
    Optional<User> findByOauthProviderAndOauthProviderId(String oauthProvider, String oauthProviderId);

    /**
     * 사용자 검색 (JPQL - NULL 체크 방식)
     * 파라미터가 NULL이면 해당 조건은 무시됩니다.
     */
    @Query("SELECT u FROM User u WHERE " +
            "(:userId IS NULL OR u.userId = :userId) AND " +
            "(:emailHash IS NULL OR u.emailHash = :emailHash) AND " +
            "(:accountLocked IS NULL OR u.accountLocked = :accountLocked) AND " +
            "(:withdrawn IS NULL OR u.withdrawn = :withdrawn)")
    List<User> findUsersByCondition(
            @Param("userId") String userId,
            @Param("emailHash") String emailHash,
            @Param("accountLocked") String accountLocked,
            @Param("withdrawn") String withdrawn
    );
}
