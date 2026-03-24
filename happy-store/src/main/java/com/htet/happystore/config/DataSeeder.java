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
        if (userRepository.findByEmail("admin@happystore.com").isEmpty()) {
            User admin = new User();
            admin.setFullName("Super Admin");
            admin.setEmail("admin@happystore.com");
            admin.setPhone("09123456789");

            // 🌟 Code ထဲမှာ အသေမရေးတော့ဘဲ Environment Variable ကနေ ဖတ်ခိုင်းပါမည်
            String adminPassword = System.getenv("ADMIN_INITIAL_PASSWORD");
            if (adminPassword == null) adminPassword = "defaultPassword123"; // Variable မရှိရင် သုံးမည့် default

            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setCountry(User.Country.MYANMAR);
            admin.setActive(true);

            userRepository.save(admin);
        }
    }
}