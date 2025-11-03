package com.koo.bonscore.common.api.mail.service;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        String title = "[Bonscore] 이메일 인증 번호 안내";
        String authCode = createAuthCode();

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("authCode", authCode);

        sendTemplateEmail(toEmail, title, "mail/verificationEmail", context);

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

    /**
     * 휴면 계정 전환 예정 안내 이메일을 발송합니다.
     *
     * @param toEmail 수신자 이메일 주소
     * @param username 받는 사람 이름
     */
    public void sendDormancyNoticeEmail(String toEmail, String username) {
        String title = "[Bonscore] 계정 휴면 전환 예정 안내";
        String subTitle = "계정 휴면 전환 예정 안내";
        String message1 = "회원님의 Bonscore 계정은 1년 이상 로그인 기록이 없어, 관련 법령에 따라 휴면 상태로 전환될 예정입니다.";
        String message2 = "예정일까지 로그인하지 않으실 경우, 회원님의 개인정보는 안전하게 분리 보관되며, 이후 서비스를 다시 이용하시려면 별도의 본인인증 절차가 필요하게 됩니다.";
        String dormantDateTitle = "휴면 전환 예정일";

        // 휴면 전환 예정일 (오늘로부터 30일 뒤)
        String dormantDate = LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));

        Context context = new Context();
        context.setVariable("title", title);
        context.setVariable("subTitle", subTitle);
        context.setVariable("username", username);
        context.setVariable("dormantDateTitle", dormantDateTitle);
        context.setVariable("dormantDate", dormantDate);
        context.setVariable("message1", message1);
        context.setVariable("message2", message2);

        sendTemplateEmail(toEmail, title, "mail/dormancyCompleteEmail", context);
    }

    /**
     * 휴면 계정 전환 완료 안내 이메일을 발송합니다.
     *
     * @param toEmail 수신자 이메일 주소
     * @param username 받는 사람 이름
     */
    public void sendDormancyCompleteEmail(String toEmail, String username) {
        String title = "[Bonscore] 계정 휴면 전환 완료 안내";
        String subTitle = "계정 휴면 전환 완료 안내";
        String message1 = "회원님의 Bonscore 계정은 1년 이상 서비스 이용 기록이 없어 관련 법령에 따라 **휴면 상태로 전환**되었으며, 회원님의 개인정보는 안전하게 분리 보관되었습니다.";
        String message2 = "서비스를 다시 이용하시려면, 로그인 페이지의 **'휴면 계정 해제'** 메뉴를 통해 본인인증 후 계정을 즉시 활성화하실 수 있습니다.";
        String dormantDateTitle = "휴면 전환일";

        // 휴면 전환일 (오늘)
        String dormantDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));

        Context context = new Context();
        context.setVariable("title", title);
        context.setVariable("subTitle", subTitle);
        context.setVariable("username", username);
        context.setVariable("dormantDateTitle", dormantDateTitle);
        context.setVariable("dormantDate", dormantDate);
        context.setVariable("message1", message1);
        context.setVariable("message2", message2);

        sendTemplateEmail(toEmail, title, "mail/dormancyCompleteEmail", context);
    }

    /**
     * 이메일 발송 공통 로직
     *
     * @param toEmail 수신자
     * @param title 제목
     * @param templateName 템플릿 경로 및 이름
     * @param context Thymeleaf 컨텍스트
     */
    private void sendTemplateEmail(String toEmail, String title, String templateName, Context context) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromEmail);
            helper.setSubject(title);

            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            log.info("템플릿 이메일 발송 성공: to={}, template={}", toEmail, templateName);

        } catch (MessagingException e) {
            log.error("템플릿 이메일 발송 실패: to={}, template={}, error={}", toEmail, templateName, e.getMessage());
            // 실무에서는 여기서 Slack 알림 등을 통해 에러를 인지할 수 있도록 처리하는 것이 좋습니다.
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다.", e);
        }
    }
}