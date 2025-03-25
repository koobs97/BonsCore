package com.koo.bonscore.biz.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;

@RestController
@RequestMapping("api")
public class RSAController {
    private final KeyPair keyPair;

    public RSAController() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        this.keyPair = keyPairGenerator.generateKeyPair();
    }

    @GetMapping("/public-key")
    public String getPublicKey() {
        PublicKey publicKey = keyPair.getPublic();
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String decrypt(String encryptedPassword) throws Exception {
        // 암호화된 데이터를 Base64로 디코딩
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPassword);

        // RSA 복호화
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        // UTF-8로 변환 후 반환
        return new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
