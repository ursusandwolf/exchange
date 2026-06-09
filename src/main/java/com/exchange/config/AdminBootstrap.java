package com.exchange.config;

import com.exchange.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class AdminBootstrap implements CommandLineRunner {

    private final AccountService accountService;

    @Value("${app.admin.bootstrap.username:admin}")
    private String adminUsername;

    @Value("${app.admin.bootstrap.password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        accountService.ensureAdminUser(adminUsername, adminPassword);
    }
}
