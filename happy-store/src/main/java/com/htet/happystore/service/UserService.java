package com.htet.happystore.service;

import com.htet.happystore.dto.RegisterRequest;
import com.htet.happystore.dto.UserProfileRequest;
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
    public User registerUser(RegisterRequest request) {

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password မထည့်ရသေးပါ");
        }

        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        String phone = request.getPhone() != null ? request.getPhone().trim() : null;

        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phone != null && !phone.isBlank();

        if (!hasEmail && !hasPhone) {
            throw new IllegalArgumentException("Email သို့မဟုတ် ဖုန်းနံပါတ် တစ်ခုခု ထည့်ပေးပါ");
        }

        if (hasEmail && userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("ဒီ Email နဲ့ အကောင့်ရှိပြီးသားပါ");
        }

        if (hasPhone && userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("ဒီ ဖုန်းနံပါတ်နဲ့ အကောင့်ရှိပြီးသားပါ");
        }

        // country String → User.Country enum convert
        User.Country country;
        try {
            country = User.Country.valueOf(request.getCountry().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Country သည် MYANMAR သို့မဟုတ် VIETNAM ဖြစ်ရမည်");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(hasEmail ? email : null);
        user.setPhone(hasPhone ? phone : null);
        user.setAddress(request.getAddress());
        user.setCountry(country);
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(User currentUser, UserProfileRequest request) {
        currentUser.setFullName(request.getFullName());
        currentUser.setPhone(request.getPhone());
        currentUser.setAddress(request.getAddress());
        currentUser.setCountry(request.getCountry());

        return userRepository.save(currentUser);
    }

    @Transactional
    public void updateProfileImage(User currentUser, String imageUrl) {
        currentUser.setProfileImageUrl(imageUrl);
        userRepository.save(currentUser);
    }
}