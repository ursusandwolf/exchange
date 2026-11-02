package com.exchange.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetMailer {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.security.password-reset.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public void sendResetEmail(String email, String token) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        // Fragment keeps the token out of server access logs / Referer for same-origin navigations.
        String resetLink = frontendUrl + "/reset-password#token=" + token;

        if (mailSender == null || mailHost == null || mailHost.isBlank()) {
            log.info("Password reset requested for {} (mail disabled; reset token not logged)", maskEmail(email));
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password reset");
        message.setText("Use this link to reset your password: " + resetLink);
        mailSender.send(message);
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "(blank)";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
