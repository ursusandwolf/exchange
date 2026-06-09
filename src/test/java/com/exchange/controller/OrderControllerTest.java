package com.exchange.controller;

import com.exchange.dto.AuthUser;
import com.exchange.dto.OcoOrderRequest;
import com.exchange.dto.OcoOrderResponse;
import com.exchange.enums.Side;
import com.exchange.config.JwtAuthenticationFilter;
import com.exchange.model.User;
import com.exchange.service.AccountService;
import com.exchange.service.MatchingManager;
import com.exchange.service.OrderService;
import com.exchange.mapper.OrderMapper;
import com.exchange.mapper.TradeMapper;
import com.exchange.repository.UserRepository;
import com.exchange.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.exchange.exception.GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @MockBean
    private OrderService orderService;

    @MockBean
    private MatchingManager matchingManager;

    @MockBean
    private AccountService accountService;

    @MockBean
    private TradeMapper tradeMapper;

    @MockBean
    private OrderMapper orderMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void submitOcoOrder_returnsGroupIdAndOrderIds() throws Exception {
        AuthUser principal = new AuthUser("user-1", "alice", "encoded-pass", false, 0);
        User user = new User("alice", "encoded-pass");
        OcoOrderRequest request = new OcoOrderRequest(
                "BTC",
                "USDT",
                Side.SELL,
                new BigDecimal("1"),
                new BigDecimal("45000"),
                new BigDecimal("35000"),
                new BigDecimal("34900")
        );
        OcoOrderResponse response = new OcoOrderResponse("group-1", List.of("order-1", "order-2"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, AuthorityUtils.NO_AUTHORITIES)
        );

        given(accountService.findById(eq("user-1"))).willReturn(java.util.Optional.of(user));
        given(orderService.submitOcoOrder(eq(user), any(OcoOrderRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/orders/oco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocoGroupId").value("group-1"))
                .andExpect(jsonPath("$.orderIds[0]").value("order-1"))
                .andExpect(jsonPath("$.orderIds[1]").value("order-2"));

        verify(orderService).submitOcoOrder(eq(user), any(OcoOrderRequest.class));
    }
}
