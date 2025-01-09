package com.koo.bonscore.core.config.enc;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.salt.RandomSaltGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Jasypt 암호화 모듈
 * - DataSource 패스워드 암호화 기능
 * - 정적 Key 사용
 *
 * @author KooBonSang
 * @version 1.0
 * @since 2025.01.09
 * */

@Configuration
public class JasyptConfig {

    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setPassword("Ro2hxvcDPUDIAOtkm5VnTQE82QOm2Thg8x3hcrYuaCE=");
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        encryptor.setPoolSize(2);
        encryptor.setSaltGenerator(new RandomSaltGenerator());

        return encryptor;
    }

    /**
     * Jasypt에서 사용할 동적 Key 생성
     *
     * @return Key
     * */
    public String keyGenerator() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[32]; // 256비트 AES 키
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
