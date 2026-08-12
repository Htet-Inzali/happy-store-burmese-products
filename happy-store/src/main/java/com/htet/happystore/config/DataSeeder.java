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
            // 🌟 အကောင့်ရှိပြီးသားဆိုလျှင် — password ကို restart တိုင်း အလိုအလျောက် မပြောင်းတော့ပါ။
            // (admin က password ကိုယ်တိုင်ပြောင်းထားရင် တည်တံ့နေစေရန်)
            // password မေ့/ပြန်ချင်မှသာ — Render env ထဲ ADMIN_FORCE_RESET=true ထည့်ပြီး redeploy လုပ်ပါ။
            String forceReset = System.getenv("ADMIN_FORCE_RESET");
            if ("true".equalsIgnoreCase(forceReset)) {
                User admin = existingAdmin.get();
                admin.setPassword(passwordEncoder.encode(adminPassword));
                userRepository.save(admin);
                System.out.println("⚠️ Admin password FORCE RESET to ENV value (ADMIN_FORCE_RESET=true).");
            } else {
                System.out.println("ℹ️ Admin account already exists — password left unchanged.");
            }
        }
    }
}