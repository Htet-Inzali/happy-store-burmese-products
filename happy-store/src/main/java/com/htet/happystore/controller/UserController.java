package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.UserDTO;
import com.htet.happystore.entity.User;
import com.htet.happystore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<User>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(getUser(userDetails), "Profile အချက်အလက်။"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserDTO.ProfileRequest request) {
        User user = getUser(userDetails);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        if(request.getCountry() != null) user.setCountry(request.getCountry());

        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(null, "Profile ပြင်ဆင်ခြင်း အောင်မြင်ပါသည်။"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success(null, "Logout အောင်မြင်ပါသည်။"));
    }

    private User getUser(UserDetails details) {
        return userRepository.findByEmail(details.getUsername())
                .or(() -> userRepository.findByPhone(details.getUsername()))
                .orElseThrow(() -> new IllegalArgumentException("User မတွေ့ပါ"));
    }
}