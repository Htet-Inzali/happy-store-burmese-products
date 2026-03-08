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
}