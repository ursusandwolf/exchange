package com.exchange.controller;

import com.exchange.dto.LoginRequest;
import com.exchange.dto.PasswordResetConfirmRequest;
import com.exchange.dto.PasswordResetRequest;
import com.exchange.dto.RegisterRequest;
import com.exchange.mapper.UserMapper;
import com.exchange.model.User;
import com.exchange.repository.UserRepository;
import com.exchange.config.JwtAuthenticationFilter;
import com.exchange.service.AccountService;
import com.exchange.service.AuthRateLimiter;
import com.exchange.service.JwtService;
import com.exchange.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.exchange.exception.GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private AuthRateLimiter authRateLimiter;

    @Test
    void login_returns401_onBadCredentials() throws Exception {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any());

        LoginRequest request = new LoginRequest("alice", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns429_whenRateLimited() throws Exception {
        doThrow(new com.exchange.exception.TooManyRequestsException("Too many login attempts. Please try again later."))
                .when(authRateLimiter)
                .assertLoginAllowed(anyString(), anyString());

        LoginRequest request = new LoginRequest("alice", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void register_returnsToken() throws Exception {
        User user = new User("alice", "encoded-password");
        given(accountService.registerUser(eq("alice"), eq("alice@example.com"), eq("secret123"))).willReturn(user);
        given(jwtService.generateToken(user.getId(), "alice", user.getTokenVersion())).willReturn("token-value");
        given(userMapper.toAuthResponse(user, "token-value"))
                .willReturn(new com.exchange.dto.AuthResponse("token-value", "alice", user.getId(), false));

        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "secret123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-value"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.userId").value(user.getId()));
    }

    @Test
    void passwordResetRequest_returnsAccepted() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("alice@example.com");

        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    void passwordResetConfirm_returnsNoContent() throws Exception {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("reset-token", "newpass123");

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}
