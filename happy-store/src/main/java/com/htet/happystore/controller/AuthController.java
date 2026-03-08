package com.htet.happystore.controller;

import com.htet.happystore.config.JwtUtils;
import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.dto.AuthDTO;
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
    public ResponseEntity<ApiResponse<AuthDTO.Response>> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        User savedUser = userService.registerUser(request);
        String token = jwtUtils.generateToken(savedUser.getEmail() != null ? savedUser.getEmail() : savedUser.getPhone());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new AuthDTO.Response(token, savedUser.getRole().name()), "အကောင့်သစ်ဖွင့်ခြင်း အောင်မြင်ပါသည်။"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDTO.Response>> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getCredential())
                .or(() -> userRepository.findByPhone(request.getCredential()));

        if (userOpt.isPresent() && passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            User user = userOpt.get();
            if (!user.isActive()) throw new IllegalStateException("သင့်အကောင့်အား ယာယီပိတ်ထားပါသည်။");

            String token = jwtUtils.generateToken(user.getEmail() != null ? user.getEmail() : user.getPhone());
            return ResponseEntity.ok(ApiResponse.success(new AuthDTO.Response(token, user.getRole().name()), "Login ဝင်ခြင်း အောင်မြင်ပါသည်။"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("အကောင့် သို့မဟုတ် စကားဝှက် မှားယွင်းနေပါသည်။"));
    }
}