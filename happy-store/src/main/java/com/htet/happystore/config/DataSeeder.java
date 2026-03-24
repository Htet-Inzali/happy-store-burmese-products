package com.htet.happystore.config;

import com.htet.happystore.entity.Role;
import com.htet.happystore.entity.User;
import com.htet.happystore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 🌟 Admin Email ရှိမရှိ အရင်စစ်မည်
        if (userRepository.findByEmail("admin@happystore.com").isEmpty()) {

            // 🌟 Render settings ထဲက "ADMIN_INITIAL_PASSWORD" ကို လှမ်းယူမည်
            String adminPassword = System.getenv("ADMIN_INITIAL_PASSWORD");
            if (adminPassword == null) adminPassword = "defaultAdmin123"; // Variable မရှိလျှင် သုံးမည့် fallback

            User admin = new User();
            admin.setFullName("Super Admin");
            admin.setEmail("admin@happystore.com");
            admin.setPhone("09123456789");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN); //
            admin.setCountry(User.Country.MYANMAR); //
            admin.setActive(true);

            userRepository.save(admin);
            System.out.println("✅ Admin account has been seeded successfully.");
        }
    }
}