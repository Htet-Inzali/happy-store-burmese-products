package com.htet.happystore.service;

import com.htet.happystore.dto.AuthDTO;
import com.htet.happystore.entity.Role;
import com.htet.happystore.entity.User;
import com.htet.happystore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(AuthDTO.RegisterRequest request) {
        if (request.getEmail() != null && userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken");
        }
        if (request.getPhone() != null && userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Phone number is already taken");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAddress(request.getAddress());
        user.setCountry(User.Country.valueOf(request.getCountry().toUpperCase()));
        user.setRole(Role.USER);
        user.setActive(true);

        return userRepository.save(user);
    }

    @Transactional
    public void updateProfileImage(User currentUser, String imageUrl) {
        currentUser.setProfileImageUrl(imageUrl);
        userRepository.save(currentUser);
    }

    // 🌟 လက်ရှိ login ဝင်ထားသူ (user/admin) ကိုယ်တိုင် password ပြောင်းခြင်း — လက်ရှိ password မှန်မှသာ
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User မတွေ့ပါ"));
        if (currentPassword == null || newPassword == null) {
            throw new IllegalArgumentException("Password အချက်အလက် မပြည့်စုံပါ။");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("လက်ရှိ password မှားနေပါသည်။");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Password အသစ်သည် အနည်းဆုံး ၆ လုံး ဖြစ်ရပါမည်။");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 🌟 Admin က ဖောက်သည် (user) တစ်ဦး၏ password ကို reset ပေးခြင်း — forgot-password အစား
    @Transactional
    public void adminResetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password အသစ်သည် အနည်းဆုံး ၆ လုံး ဖြစ်ရပါမည်။");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User မတွေ့ပါ"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}