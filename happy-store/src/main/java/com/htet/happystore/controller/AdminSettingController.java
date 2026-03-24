package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.entity.GlobalSetting;
import com.htet.happystore.repository.GlobalSettingRepository;
import com.htet.happystore.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
// 🌟 Customer တွေပါ Cart Page မှာ လှမ်းယူရမှာဖြစ်လို့ "/api/settings" ဟု ပြောင်းသုံးပါမည်
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class AdminSettingController {

    private final SettingService settingService;
    private final GlobalSettingRepository globalSettingRepository;

    // 🌟 Frontend မှ ဆက်တင်များကို ပြန်ယူရန် GET API အသစ်
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getAllSettings() {
        Map<String, BigDecimal> settingsMap = globalSettingRepository.findAll().stream()
                .collect(Collectors.toMap(GlobalSetting::getSettingKey, GlobalSetting::getSettingValue));
        return ResponseEntity.ok(ApiResponse.success(settingsMap, "Settings ရရှိပါပြီ။"));
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<String>> updateSettings(@RequestBody Map<String, BigDecimal> settings) {
        settings.forEach(settingService::updateSetting);
        return ResponseEntity.ok(ApiResponse.success(null, "Settings များ အောင်မြင်စွာ Update လုပ်ပြီးပါပြီ။"));
    }
}