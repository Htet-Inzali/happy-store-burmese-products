// ဖိုင်တည်နေရာ: src/main/java/com/htet/happystore/config/WebConfig.java

package com.htet.happystore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 🌟 ပြင်ဆင်ချက်: Server ပေါ်တွင် လမ်းကြောင်းမလွဲစေရန် System ၏ Current Directory ကို အခြေခံ၍ Absolute Path တည်ဆောက်ပါသည်
        String userDir = System.getProperty("user.dir");
        String location = "file:" + Paths.get(userDir, uploadDir).toAbsolutePath().normalize() + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}