//package com.koo.bonscore.core.config.enc;
//
//
//import org.jasypt.encryption.StringEncryptor;
//import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
//import org.jasypt.salt.RandomSaltGenerator;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.security.SecureRandom;
//import java.util.Base64;
//
///**
// * Jasypt 암호화 모듈
// * - DataSource 패스워드 암호화 기능
// * - 정적 Key 사용
// *
// * @author KooBonSang
// * @version 1.0
// * @since 2025.01.09
// * */
//
//@Configuration
//public class JasyptConfig {
//
//    // 1. 여기에 새로 생성한 Secret Key를 넣어주세요.
//    private static final String SECRET_KEY = ""; // 예: 이전에 생성한 키
//
//    @Bean("jasyptStringEncryptor")
//    public StringEncryptor stringEncryptor() {
//        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
//        encryptor.setPassword(SECRET_KEY); // 설정된 Secret Key 사용
//        encryptor.setAlgorithm("PBEWithMD5AndDES");
//        encryptor.setPoolSize(2);
//        encryptor.setSaltGenerator(new RandomSaltGenerator());
//
//        return encryptor;
//    }
//
//    /**
//     * Jasypt에서 사용할 동적 Key를 생성합니다.
//     * 필요할 때 이 메소드를 실행해서 새로운 키를 얻으세요.
//     */
//    public static void generateNewKey() {
//        SecureRandom random = new SecureRandom();
//        byte[] key = new byte[32]; // 256비트 AES 키
//        random.nextBytes(key);
//        System.out.println("New Secret Key : " + Base64.getEncoder().encodeToString(key));
//    }
//
//    /**
//     * 실제 비밀번호를 암호화하기 위한 메소드입니다.
//     */
//    public static void main(String[] args) {
//        // --- STEP 1: 새로운 Secret Key가 필요하면 아래 메소드의 주석을 풀고 실행 ---
//        // generateNewKey();
//
//        // --- STEP 2: 실제 DB 비밀번호를 암호화하려면 아래 코드를 실행 ---
//        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
//        encryptor.setPassword(SECRET_KEY); // 반드시 Bean에 설정된 것과 동일한 키를 사용해야 합니다.
//        encryptor.setAlgorithm("PBEWithMD5AndDES");
//        encryptor.setPoolSize(2);
//        encryptor.setSaltGenerator(new RandomSaltGenerator());
//
//        // 2. 여기에 암호화하고 싶은 실제 DB 비밀번호를 입력하세요.
//        String plainTextPassword = ""; // 예: "1234"
//        String encryptedPassword = encryptor.encrypt(plainTextPassword);
//
//        System.out.println("====================================");
//        System.out.println("Encrypted Password : " + encryptedPassword);
//        System.out.println("====================================");
//    }
//}
