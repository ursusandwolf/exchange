package com.exchange.controller;

import com.exchange.dto.AuthResponse;
import com.exchange.dto.PasswordResetConfirmRequest;
import com.exchange.dto.PasswordResetRequest;
import com.exchange.dto.LoginRequest;
import com.exchange.dto.RegisterRequest;
import com.exchange.mapper.UserMapper;
import com.exchange.model.User;
import com.exchange.service.AccountService;
import com.exchange.service.AuthRateLimiter;
import com.exchange.service.JwtService;
import com.exchange.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AccountService accountService;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;
    private final AuthRateLimiter authRateLimiter;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request, HttpServletRequest httpRequest) {
        authRateLimiter.assertRegisterAllowed(clientKey(httpRequest), request.username(), request.email());
        User user = accountService.registerUser(request.username(), request.email(), request.password());
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());
        return ResponseEntity.ok(userMapper.toAuthResponse(user, token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        authRateLimiter.assertLoginAllowed(clientKey(httpRequest), request.username());
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        
        User user = accountService.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());
        authRateLimiter.clearLogin(clientKey(httpRequest), request.username());
        return ResponseEntity.ok(userMapper.toAuthResponse(user, token));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody @Valid PasswordResetRequest request, HttpServletRequest httpRequest) {
        authRateLimiter.assertPasswordResetAllowed(clientKey(httpRequest), request.email());
        passwordResetService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@RequestBody @Valid PasswordResetConfirmRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
