package com.htet.happystore.config;

import com.htet.happystore.entity.Role;
import com.htet.happystore.entity.User;
import com.htet.happystore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@happystore.com";
        String adminPassword = System.getenv("ADMIN_INITIAL_PASSWORD");
        if (adminPassword == null) adminPassword = "defaultAdmin123";

        Optional<User> existingAdmin = userRepository.findByEmail(adminEmail);

        if (existingAdmin.isEmpty()) {
            // အကောင့်မရှိသေးလျှင် အသစ်ဆောက်မည်
            User admin = new User();
            admin.setFullName("Super Admin");
            admin.setEmail(adminEmail);
            admin.setPhone("09123456789");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setCountry(User.Country.MYANMAR);
            admin.setActive(true);
            userRepository.save(admin);
            System.out.println("✅ Admin account created with password from ENV.");
        } else {
            // 🌟 အရေးကြီး - အကောင့်ရှိပြီးသားဆိုလျှင် Password ကို ENV ထဲကအတိုင်း Force Update လုပ်မည်
            User admin = existingAdmin.get();
            admin.setPassword(passwordEncoder.encode(adminPassword));
            userRepository.save(admin);
            System.out.println("✅ Admin password has been FORCE RESET to ENV value.");
        }
    }
}