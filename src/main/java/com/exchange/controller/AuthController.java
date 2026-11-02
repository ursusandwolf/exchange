package com.exchange.controller;

import com.exchange.config.AuthCookieService;
import com.exchange.config.ClientIpResolver;
import com.exchange.dto.AuthResponse;
import com.exchange.dto.LoginRequest;
import com.exchange.dto.PasswordResetConfirmRequest;
import com.exchange.dto.PasswordResetRequest;
import com.exchange.dto.RegisterRequest;
import com.exchange.mapper.UserMapper;
import com.exchange.model.User;
import com.exchange.service.AccountService;
import com.exchange.service.AuthRateLimiter;
import com.exchange.service.JwtService;
import com.exchange.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final ClientIpResolver clientIpResolver;
    private final AuthCookieService authCookieService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody @Valid RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        authRateLimiter.assertRegisterAllowed(clientIpResolver.resolve(httpRequest), request.username(), request.email());
        User user = accountService.registerUser(request.username(), request.email(), request.password());
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());
        authCookieService.write(httpResponse, token);
        return ResponseEntity.ok(userMapper.toAuthResponse(user, token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientKey = clientIpResolver.resolve(httpRequest);
        authRateLimiter.assertLoginAllowed(clientKey, request.username());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = accountService.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());
        authRateLimiter.clearLogin(clientKey, request.username());
        authCookieService.write(httpResponse, token);
        return ResponseEntity.ok(userMapper.toAuthResponse(user, token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
        authCookieService.clear(httpResponse);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(
            @RequestBody @Valid PasswordResetRequest request,
            HttpServletRequest httpRequest
    ) {
        authRateLimiter.assertPasswordResetAllowed(clientIpResolver.resolve(httpRequest), request.email());
        passwordResetService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @RequestBody @Valid PasswordResetConfirmRequest request,
            HttpServletRequest httpRequest
    ) {
        authRateLimiter.assertPasswordResetConfirmAllowed(clientIpResolver.resolve(httpRequest), request.token());
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
