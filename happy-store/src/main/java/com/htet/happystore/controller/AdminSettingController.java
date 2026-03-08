package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingController {

    private final SettingService settingService;

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<String>> updateSettings(@RequestBody Map<String, BigDecimal> settings) {
        settings.forEach(settingService::updateSetting);
        return ResponseEntity.ok(ApiResponse.success(null, "Settings များ အောင်မြင်စွာ Update လုပ်ပြီးပါပြီ။"));
    }
}