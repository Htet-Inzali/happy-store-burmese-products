package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.OrderRequest;
import com.htet.happystore.dto.OrderUserResponse;
import com.htet.happystore.entity.Order;
import com.htet.happystore.entity.User;
import com.htet.happystore.repository.UserRepository;
import com.htet.happystore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<String>> checkout(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String credential = userDetails.getUsername();
        User user = userRepository.findByEmail(credential)
                .orElseGet(() -> userRepository.findByPhone(credential)
                        .orElseThrow(() -> new IllegalStateException("User not found")));

        Order order = orderService.placeOrder(user, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Order တင်ခြင်း အောင်မြင်ပါသည်။ ID: " + order.getId()));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<OrderUserResponse>>> getMyOrders(@AuthenticationPrincipal User user) {
        List<OrderUserResponse> orders = orderService.getMyOrders(user);
        return ResponseEntity.ok(ApiResponse.success(orders, "ဝယ်ယူမှုမှတ်တမ်းများကို ရရှိပါပြီ။"));
    }
}