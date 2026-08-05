package com.htet.happystore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Admin ကို Telegram မှတစ်ဆင့် အသိပေးချက် (order အသစ် စသည်) ပို့သည်။
 * best-effort — Telegram fail ဖြစ်လျှင်လည်း order flow ကို block/fail မဖြစ်စေပါ။
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.chat-id:}")
    private String chatId;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** Telegram message ကို background thread တွင် ပို့သည် (order response ကို မနှောင့်နှေးစေရန်)။ */
    public void sendTelegram(String message) {
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            return; // မ config ရသေးပါ — တိတ်တဆိတ် ကျော်သွားသည်
        }
        CompletableFuture.runAsync(() -> {
            try {
                String url = "https://api.telegram.org/bot" + botToken + "/sendMessage"
                        + "?chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8)
                        + "&parse_mode=HTML"
                        + "&text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) {
                    log.warn("Telegram alert failed ({}): {}", res.statusCode(), res.body());
                }
            } catch (Exception e) {
                log.warn("Telegram alert error: {}", e.getMessage());
            }
        });
    }
}
