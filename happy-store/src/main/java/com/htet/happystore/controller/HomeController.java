package com.htet.happystore.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Happy Store Backend Running";
    }

    // 🌟 Keep-warm ping endpoint — DB မထိဘဲ ပေါ့ပါးစွာ ပြန်ပေးသည် (cron ping အတွက်)
    @GetMapping("/api/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "status", "ok",
                "time", System.currentTimeMillis()
        );
    }
}