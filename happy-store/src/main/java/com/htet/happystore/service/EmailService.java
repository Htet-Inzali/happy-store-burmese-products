package com.htet.happystore.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:no-reply@happystore.com}}")
    private String fromAddress;

    // 🌟 Password reset link ကို email ဖြင့် ပို့သည် (မြန်မာစာ UTF-8)
    public void sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("HappyStore — စကားဝှက် ပြန်လည်သတ်မှတ်ရန်");

            String body = "မင်္ဂလာပါ " + (userName != null ? userName : "") + ",\n\n"
                    + "သင့် HappyStore အကောင့်၏ စကားဝှက်ကို ပြန်လည်သတ်မှတ်ရန် တောင်းဆိုထားပါသည်။\n"
                    + "အောက်ပါ link ကို နှိပ်၍ စကားဝှက်အသစ် သတ်မှတ်ပါ (၃၀ မိနစ်အတွင်း အသုံးပြုရပါမည်):\n\n"
                    + resetLink + "\n\n"
                    + "အကယ်၍ သင် ဤတောင်းဆိုမှုကို မလုပ်ခဲ့ပါက ဤ email ကို လျစ်လျူရှုနိုင်ပါသည်။\n\n"
                    + "— HappyStore";

            helper.setText(body, false);
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            // Email ပို့မရလျှင် process မရပ်စေဘဲ log သာ မှတ်သည် (enumeration မဖော်ရန် endpoint က success ပြန်နေမည်)
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
