package com.htet.happystore.controller;

import com.htet.happystore.config.JwtUtils;
import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.AuthResponse;
import com.htet.happystore.dto.LoginRequest;
import com.htet.happystore.dto.RegisterRequest;
import com.htet.happystore.entity.User;
import com.htet.happystore.repository.UserRepository;
import com.htet.happystore.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "အကောင့်ဖွင့်ခြင်း အောင်မြင်ပါသည်။"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getCredential())
                .or(() -> userRepository.findByPhone(request.getCredential()));

        if (userOpt.isPresent() && passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            User user = userOpt.get();
            String token = jwtUtils.generateToken(user.getEmail() != null ? user.getEmail() : user.getPhone());
            AuthResponse data = new AuthResponse(token, user.getRole().name());
            return ResponseEntity.ok(ApiResponse.success(data, "Login အောင်မြင်ပါသည်။"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email/Phone သို့မဟုတ် Password မှားယွင်းနေပါသည်။"));
    }
}