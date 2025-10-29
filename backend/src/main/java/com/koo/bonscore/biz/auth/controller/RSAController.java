package com.koo.bonscore.biz.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;

/**
 * <pre>
 * RSAController.java
 * 설명 : 암호화 유틸
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-03-25
 */
@RestController
@RequestMapping("api")
public class RSAController {
    private final KeyPair keyPair;

    /**
     * 생성자
     *
     * @throws Exception e
     */
    public RSAController() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        this.keyPair = keyPairGenerator.generateKeyPair();
    }

    /**
     * 파라미터를 암호화 할 공개키 get
     *
     * @return 공개키
     */
    @GetMapping("/public-key")
    public String getPublicKey() {
        PublicKey publicKey = keyPair.getPublic();
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * 복호화 메소드
     * @param encryptedPassword
     * @return
     * @throws Exception
     */
    public String decrypt(String encryptedPassword) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPassword);

        // RSA 복호화
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        // UTF-8로 변환 후 반환
        return new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
