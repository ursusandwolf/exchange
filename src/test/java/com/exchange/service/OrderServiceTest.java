package com.exchange.service;

import com.alex.fin.core.domain.common.InstrumentId;
import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import com.exchange.dto.OrderRequest;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.Order;
import com.exchange.model.User;
import com.exchange.engine.OrderBook;
import com.exchange.repository.OrderRepository;
import com.exchange.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MatchingManager matchingManager;
    @Mock
    private OrderBook orderBook;
    @Mock
    private MarketDataService marketDataService;
    @Mock
    private FeeService feeService;
    @Mock
    private EntityManager entityManager;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private WalletService walletService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                userRepository,
                matchingManager,
                marketDataService,
                feeService,
                entityManager,
                transactionTemplate,
                walletService
        );
        orderService.init();
    }

    @Test
    void submitOrder_shouldSaveOrderAndReserveFunds() {
        User user = new User("user", "pass");
        // User ID is generated in constructor, can use user.getId() if needed
        OrderRequest request = new OrderRequest("BTC", "USD", Side.BUY, OrderType.LIMIT, new BigDecimal("1"), new BigDecimal("50000"), null, null);

        // Stubbing transactionTemplate to execute immediately
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            var action = invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return action.doInTransaction(null);
        });
        
        when(feeService.calculateTakerFee(any())).thenReturn(BigDecimal.ZERO);
        when(matchingManager.getOrderBook(anyString(), anyString())).thenReturn(orderBook);

        orderService.submitOrder(user, request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getAllValues().get(0);

        assertThat(savedOrder.getUserId()).isEqualTo(user.getId());
        assertThat(savedOrder.getSide()).isEqualTo(Side.BUY);
        assertThat(savedOrder.getType()).isEqualTo(OrderType.LIMIT);
        assertThat(savedOrder.getQuantity().value()).isEqualTo(new BigDecimal("1"));
        verify(walletService).reserve(eq(user.getId()), eq("USD"), any(), anyString());
    }
}
