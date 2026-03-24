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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    // 🌟 Spring Security အစစ်မှ စစ်ဆေးရန် AuthenticationManager ကို ထပ်တိုးထားသည်
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDTO.Response>> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        User savedUser = userService.registerUser(request);
        String token = jwtUtils.generateToken(savedUser.getEmail() != null ? savedUser.getEmail() : savedUser.getPhone());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new AuthDTO.Response(token, savedUser.getRole().name()), "အကောင့်သစ်ဖွင့်ခြင်း အောင်မြင်ပါသည်။"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDTO.Response>> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        try {
            // 🌟 ၁။ Spring Security အစစ်မှ တစ်ဆင့် Credentials မှန်/မမှန် အရင်စစ်ဆေးခြင်း (Vulnerability ကာကွယ်ရန်)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCredential(), request.getPassword())
            );

            // 🌟 ၂။ Authentication အောင်မြင်သွားပါက Database မှ User အချက်အလက် ပြန်ယူခြင်း
            Optional<User> userOpt = userRepository.findByEmail(request.getCredential())
                    .or(() -> userRepository.findByPhone(request.getCredential()));

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // အကောင့်ပိတ်ခံထားရခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
                if (!user.isActive()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("သင့်အကောင့်အား ယာယီပိတ်ထားပါသည်။ Admin သို့ ဆက်သွယ်ပါ။"));
                }

                // 🌟 ၃။ အားလုံးအဆင်ပြေပါက Token ထုတ်ပေးခြင်း
                String token = jwtUtils.generateToken(user.getEmail() != null ? user.getEmail() : user.getPhone());
                return ResponseEntity.ok(ApiResponse.success(new AuthDTO.Response(token, user.getRole().name()), "Login ဝင်ခြင်း အောင်မြင်ပါသည်။"));
            }
        } catch (Exception e) {
            // 🌟 Exception မိခဲ့လျှင် (Password မှားခြင်း၊ အကောင့်မရှိခြင်း စသည်)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("အကောင့် သို့မဟုတ် စကားဝှက် မှားယွင်းနေပါသည်။"));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("အကောင့် သို့မဟုတ် စကားဝှက် မှားယွင်းနေပါသည်။"));
    }

    // 🌟 Database အတွက် Password အမှန်ရယူရန် ဖြတ်လမ်း API (Testing အတွက်)
    @GetMapping("/generate-hash")
    public String generateHash(@RequestParam String password) {
        return passwordEncoder.encode(password);
    }
}