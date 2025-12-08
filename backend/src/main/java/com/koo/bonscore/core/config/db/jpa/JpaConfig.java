package com.koo.bonscore.core.config.db.jpa;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * <pre>
 * JpaConfig.java
 * 설명 : JPA 관련 추가 설정을 관리하는 클래스
 *       - JPA Auditing 활성화 (@EnableJpaAuditing)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-11-24
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    /*
     * Spring Boot의 AutoConfiguration에 의해 EntityManagerFactory,
     * TransactionManager 등은 자동으로 설정되므로 별도의 Bean 등록 코드가 불필요함.
     *
     * 필요 시 QueryDSL 설정이나 AuditorAware(등록자/수정자 자동 주입) 빈을 여기에 추가.
     */
}
