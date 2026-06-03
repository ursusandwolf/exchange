package com.exchange.service;

import com.exchange.model.User;
import com.exchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Пароль должен быть не менее 6 символов");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }
        User user = new User(username, passwordEncoder.encode(password));
        
        // Стартовый капитал
        user.getWallet().credit("USDT", new BigDecimal("50000"));
        user.getWallet().credit("BTC", new BigDecimal("1"));
        
        return userRepository.save(user);
    }

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
