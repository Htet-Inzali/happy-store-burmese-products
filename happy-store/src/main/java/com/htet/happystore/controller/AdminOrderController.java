package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.OrderDTO;
import com.htet.happystore.dto.ReportDTO;
import com.htet.happystore.service.OrderService;
import com.htet.happystore.service.SalesReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final SalesReportService salesReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDTO.AdminResponse>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrdersForAdmin(), "အော်ဒါစာရင်းအားလုံး။"));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(@PathVariable Long orderId, @RequestParam("status") String status) {
        orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.success(null, "Order Status ပြောင်းလဲခြင်း အောင်မြင်ပါသည်။"));
    }

    // 🌟 Preorder ကို Stock နှုတ်ပြီး အတည်ပြုမည့် API အသစ်
    @PostMapping("/{orderId}/fulfill")
    public ResponseEntity<ApiResponse<String>> fulfillPreorder(@PathVariable Long orderId) {
        orderService.fulfillPreorder(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Preorder အတွက် Stock နှုတ်ယူပြီး အတည်ပြုလိုက်ပါပြီ။"));
    }

    @GetMapping("/summary/today")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodaySummary() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getDailySalesSummary(), "ယနေ့ အရောင်းအနှစ်ချုပ်။"));
    }

    @GetMapping("/reports/sales")
    public ResponseEntity<ApiResponse<List<ReportDTO.Sales>>> getSalesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<ReportDTO.Sales> reports = (startDate != null && endDate != null)
                ? salesReportService.getSalesReport(startDate, endDate)
                : salesReportService.getAllSalesReport();

        return ResponseEntity.ok(ApiResponse.success(reports, "အရောင်း မှတ်တမ်းများ။"));
    }
}