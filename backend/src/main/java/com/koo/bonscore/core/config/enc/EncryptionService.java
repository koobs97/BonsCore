package com.koo.bonscore.core.config.enc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {

    private final byte[] aesKey;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTE = 12;
    private static final int TAG_LENGTH_BIT = 128;

    public EncryptionService(@Value("${security.encryption-key}") String key) {
        // Hex 문자열 키를 byte 배열로 변환
        this.aesKey = hexStringToByteArray(key);
    }

    /**
     * 데이터를 양방향 암호화합니다.
     * @param data 원본 데이터
     * @return Base64로 인코딩된 암호문 (IV 포함)
     */
    public String encrypt(String data) {
        if (data == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec);

            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // IV와 암호문을 합쳐서 Base64로 인코딩
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("데이터 암호화 중 오류 발생", e);
        }
    }

    /**
     * 암호화된 데이터를 복호화합니다. (참고용)
     * @param encryptedData Base64로 인코딩된 암호문
     * @return 원본 데이터
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null) return null;
        try {
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedData);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec);

            byte[] decryptedData = cipher.doFinal(cipherText);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("데이터 복호화 중 오류 발생", e);
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public String hashWithSalt(String data) {
        if (data == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 데이터와 솔트를 결합하여 보안 강화
            String dataWithSalt = data + Arrays.toString(aesKey);
            byte[] hashedBytes = md.digest(dataWithSalt.getBytes(StandardCharsets.UTF_8));

            // 바이트 배열을 16진수 문자열로 변환
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("데이터 해싱 중 오류 발생", e);
        }
    }

    /**
     * 파라미터를 SHA-256으로 해싱
     * @param param`
     * @return 해싱된 문자열
     */
    public String hashString(String param) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(param.getBytes(StandardCharsets.UTF_8));

            // 바이트 배열을 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // 이 예외는 발생해서는 안 됩니다. SHA-256은 표준 알고리즘입니다.
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
