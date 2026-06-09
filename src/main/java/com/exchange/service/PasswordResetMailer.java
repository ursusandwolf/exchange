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
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        if (mailSender == null || mailHost == null || mailHost.isBlank()) {
            log.info("Password reset requested for {}, token: {}", email, token);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password reset");
        message.setText("Use this link to reset your password: " + resetLink);
        mailSender.send(message);
    }
}
