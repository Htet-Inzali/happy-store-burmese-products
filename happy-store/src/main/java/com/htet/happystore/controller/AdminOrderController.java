package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.OrderAdminResponse;
import com.htet.happystore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderAdminResponse>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrdersForAdmin(), "Order စာရင်းများ ရရှိပါပြီ။"));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(@PathVariable Long orderId, @RequestParam("status") String status) {
        orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.success(null, "Order Status ပြောင်းလဲခြင်း အောင်မြင်ပါသည်။"));
    }

    @GetMapping("/summary/today")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodaySummary() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getDailySalesSummary(), "ယနေ့ အရောင်းအနှစ်ချုပ်။"));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderAdminResponse>> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderDetailsForAdmin(orderId), "Order အသေးစိတ် အချက်အလက်။"));
    }
}