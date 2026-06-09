package com.exchange.controller;

import com.exchange.model.User;
import com.exchange.service.ArbitrageBotService;
import com.exchange.repository.OrderRepository;
import com.exchange.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private ArbitrageBotService arbitrageBotService;

    @Test
    void stats_areForbiddenForNonAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/stats").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void stats_areAvailableForAdminUsers() throws Exception {
        User feeUser = new User("SYSTEM_FEES", "secret", true);
        feeUser.getWallet().credit("USDT", new BigDecimal("123.45"));
        User alice = new User("alice", "secret");
        alice.getWallet().credit("USDT", new BigDecimal("100"));
        alice.getWallet().reserve("USDT", new BigDecimal("10"));

        given(accountService.findByUsername("SYSTEM_FEES")).willReturn(Optional.of(feeUser));
        given(accountService.findAllUsers()).willReturn(List.of(feeUser, alice));
        given(orderRepository.count()).willReturn(12L);
        given(orderRepository.countByStatusIn(List.of(com.exchange.enums.OrderStatus.PENDING, com.exchange.enums.OrderStatus.PARTIALLY_FILLED)))
                .willReturn(3L);

        mockMvc.perform(get("/api/admin/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.totalOrders").value(12))
                .andExpect(jsonPath("$.activeOrders").value(3))
                .andExpect(jsonPath("$.adminUsers").value(1))
                .andExpect(jsonPath("$.systemUserPresent").value(true))
                .andExpect(jsonPath("$.exchangeProfit.USDT").value(123.45));
    }

    @Test
    void updateAdminRole_changesRole() throws Exception {
        User user = new User("alice", "secret", true);
        given(accountService.setAdmin("user-1", true)).willReturn(user);

        mockMvc.perform(patch("/api/admin/users/user-1/admin")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.exchange.dto.AdminUserRoleRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin").value(true));

        verify(accountService).setAdmin("user-1", true);
    }
}
