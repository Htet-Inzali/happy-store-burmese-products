package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.DashboardDTO;
import com.htet.happystore.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardDTO.Summary>> getSummary(@RequestParam(defaultValue = "TODAY") String filter) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardSummary(filter), "Dashboard အချက်အလက်များ ရရှိပါပြီ။"));
    }

    @GetMapping("/alerts/expiring")
    public ResponseEntity<ApiResponse<List<DashboardDTO.ExpiringBatch>>> getExpiringAlerts() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getExpiringBatchesAlert(), "သက်တမ်းကုန်ခါနီး စာရင်း ရရှိပါပြီ။"));
    }

    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<List<DashboardDTO.TopProduct>>> getTopProducts() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getTopProducts(), "အရောင်းရဆုံး ပစ္စည်းများ ရရှိပါပြီ။"));
    }

    // 🌟 Excel Download အစစ်အတွက် API
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestParam(defaultValue = "TODAY") String filter) {
        byte[] file = dashboardService.generateExcelReport(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales_report_" + filter + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}