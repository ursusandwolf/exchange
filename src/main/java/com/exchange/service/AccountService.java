package com.exchange.service;

import com.exchange.model.User;
import com.exchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
    public User registerUser(String username, String email, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Пароль должен быть не менее 6 символов");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }
        User user = new User(username, passwordEncoder.encode(password), false, email);
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Пользователь с таким именем или email уже существует", e);
        }
    }

    @Transactional
    public User registerUser(String username, String password) {
        return registerUser(username, username + "@exchange.local", password);
    }

    @Transactional
    public User ensureAdminUser(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> new User(username, passwordEncoder.encode(password), true));
        if (!user.isAdmin()) {
            user.setAdmin(true);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            user.changePassword(passwordEncoder.encode(password));
            user.incrementTokenVersion();
        }
        return userRepository.saveAndFlush(user);
    }

    @Transactional
    public User setAdmin(String userId, boolean admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));
        user.setAdmin(admin);
        return userRepository.saveAndFlush(user);
    }

    public java.util.List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Пароль должен быть не менее 6 символов");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Текущий пароль неверный");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Новый пароль должен отличаться от текущего");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        user.incrementTokenVersion();
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public void seedDemoBalances(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));

        user.getWallet().credit("USDT", new BigDecimal("50000"));
        user.getWallet().credit("BTC", new BigDecimal("1"));
        userRepository.save(user);
    }

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
