package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.OrderDTO;
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
    public ResponseEntity<ApiResponse<List<OrderDTO.UserResponse>>> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderDTO.Request request) {
        User user = getUser(userDetails);
        // 🌟 အော်ဒါ ၂ ခု ခွဲထွက်သွားနိုင်သဖြင့် List အနေဖြင့် ပြန်ပို့ပေးပါမည်
        return ResponseEntity.ok(ApiResponse.success(orderService.createOrder(user, request), "အော်ဒါတင်ခြင်း အောင်မြင်ပါသည်။"));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<OrderDTO.UserResponse>>> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyOrders(user), "သင်၏ အော်ဒါမှတ်တမ်းများ။"));
    }

    // 🌟 Customer: မိမိ၏ PENDING order ကို ပယ်ဖျက်ခြင်း
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelMyOrder(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long orderId) {
        User user = getUser(userDetails);
        orderService.cancelMyOrder(user, orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "အော်ဒါကို ပယ်ဖျက်ပြီးပါပြီ။"));
    }

    private User getUser(UserDetails details) {
        return userRepository.findByEmail(details.getUsername())
                .or(() -> userRepository.findByPhone(details.getUsername()))
                .orElseThrow(() -> new IllegalArgumentException("User မတွေ့ပါ"));
    }
}