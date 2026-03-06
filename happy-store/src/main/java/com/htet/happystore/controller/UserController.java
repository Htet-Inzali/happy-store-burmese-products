package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.UserProfileRequest;
import com.htet.happystore.entity.User;
import com.htet.happystore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor // 🌟 ဒါလေး ထည့်ပေးပါ (userService အတွက်)
public class UserController {
    private final UserService userService; // 🌟 final လုပ်ပေးပါ

    @PutMapping
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UserProfileRequest request) {
        User updatedUser = userService.updateProfile(user, request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Profile ပြင်ဆင်ပြီးပါပြီ။"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success(null, "Logout အောင်မြင်ပါသည်။"));
    }
}
