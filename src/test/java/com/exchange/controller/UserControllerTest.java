package com.exchange.controller;

import com.exchange.dto.AuthUser;
import com.exchange.dto.ChangePasswordRequest;
import com.exchange.service.AccountService;
import com.exchange.service.JwtService;
import com.exchange.service.WalletService;
import com.exchange.mapper.TransactionRecordMapper;
import com.exchange.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.exchange.exception.GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @MockBean
    private AccountService accountService;

    @MockBean
    private WalletService walletService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private TransactionRecordMapper transactionRecordMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private com.exchange.config.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void changePassword_returns204_andDelegatesToService() throws Exception {
        AuthUser principal = new AuthUser("user-1", "alice", "encoded-old-pass", false, 0);
        ChangePasswordRequest request = new ChangePasswordRequest("oldpass", "newpass123");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, AuthorityUtils.NO_AUTHORITIES)
        );

        mockMvc.perform(put("/api/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(accountService).changePassword(eq("user-1"), eq("oldpass"), eq("newpass123"));
    }
}
