package com.koo.bonscore.common.mail.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine; // Spring Boot 3 / Thymeleaf 6 기준

import java.util.Random;

/**
 * <pre>
 * MailService.java
 * 설명 : HTML 템플릿 기반의 이메일 발송 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.1 (Refactored)
 * @since   : 2025-07-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    // application.yml에서 발신자 이메일 주소를 주입받습니다.
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * HTML 템플릿을 사용하여 인증 이메일을 발송합니다.
     *
     * @param toEmail 수신자 이메일 주소
     * @param username 받는 사람 이름
     * @return 생성된 6자리 인증 코드
     */
    public String sendVerificationEmail(String toEmail, String username) {
        String title = "[Bonscore] 이메일 인증 번호 안내"; // 서비스 이름에 맞게 수정
        String authCode = createAuthCode();

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromEmail);
            helper.setSubject(title);

            // Thymeleaf 컨텍스트에 템플릿 변수 설정
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("authCode", authCode);

            // Thymeleaf 템플릿 엔진을 사용하여 HTML 생성
            String htmlContent = templateEngine.process("mail/verificationEmail", context);
            helper.setText(htmlContent, true); // HTML 메일임을 명시

            javaMailSender.send(mimeMessage);
            log.info("인증 메일 발송 성공: {}", toEmail);

        } catch (MessagingException e) {
            log.error("HTML 이메일 발송 실패: toEmail={}, error={}", toEmail, e.getMessage());
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다.", e);
        }

        return authCode;
    }

    /**
     * 6자리 영문+숫자 인증 코드를 생성합니다.
     *
     * @return 6자리 인증 코드
     */
    private String createAuthCode() {
        String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder codeBuilder = new StringBuilder(6);

        for (int i = 0; i < 6; i++) {
            int randomIndex = random.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }
}