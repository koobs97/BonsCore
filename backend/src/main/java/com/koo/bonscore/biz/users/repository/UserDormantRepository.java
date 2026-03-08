package com.koo.bonscore.biz.users.repository;

import com.koo.bonscore.biz.users.entity.UserDormantInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * <pre>
 * UserDormantRepository.java
 * 설명 : 휴면 계정(UserDormantInfo) 데이터를 관리하는 저장소
 *       (장기 미접속으로 인해 별도 테이블로 분리된 회원 정보)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-13
 */
public interface UserDormantRepository extends JpaRepository<UserDormantInfo, String> {

    /**
     * 이메일 해시로 휴면 계정 찾기
     * @param emailHash 이메일 해쉬값
     * @return 휴면 계정 정보
     */
    Optional<UserDormantInfo> findByEmailHash(String emailHash);

    // 이메일 해시 + 이름으로 찾기
    Optional<UserDormantInfo> findByEmailHashAndUserName(String emailHash, String userName);
}
