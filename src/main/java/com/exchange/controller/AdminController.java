package com.exchange.controller;

import com.exchange.dto.AdminDashboardResponse;
import com.exchange.dto.AdminStatsResponse;
import com.exchange.dto.AdminUserResponse;
import com.exchange.dto.AdminUserRoleRequest;
import com.exchange.enums.OrderStatus;
import com.exchange.model.User;
import com.exchange.repository.OrderRepository;
import com.exchange.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final String SYSTEM_FEE_USER = "SYSTEM_FEES";

    private final AccountService accountService;
    private final OrderRepository orderRepository;

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        User feeUser = accountService.findByUsername(SYSTEM_FEE_USER).orElse(null);
        Map<String, BigDecimal> fees = (feeUser != null)
                ? feeUser.getWallet().getBalances()
                : Map.of();

        long totalUsers = accountService.findAllUsers().size();
        long adminUsers = accountService.findAllUsers().stream().filter(User::isAdmin).count();
        long totalOrders = orderRepository.count();
        long activeOrders = orderRepository.countByStatusIn(List.of(OrderStatus.PENDING, OrderStatus.PARTIALLY_FILLED));

        return new AdminStatsResponse(
                totalUsers,
                totalOrders,
                activeOrders,
                adminUsers,
                fees,
                feeUser != null
        );
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers() {
        return accountService.findAllUsers().stream()
                .map(user -> new AdminUserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.isAdmin(),
                        user.getWallet().getBalances(),
                        user.getWallet().getReserved()
                ))
                .toList();
    }

    @PatchMapping("/users/{userId}/admin")
    @Transactional
    public AdminUserResponse updateAdminRole(
            @PathVariable String userId,
            @RequestBody AdminUserRoleRequest request
    ) {
        User saved = accountService.setAdmin(userId, request.admin());
        return new AdminUserResponse(
                saved.getId(),
                saved.getUsername(),
                saved.isAdmin(),
                saved.getWallet().getBalances(),
                saved.getWallet().getReserved()
        );
    }
}
