package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.entity.User;
import com.htet.happystore.entity.WishlistItem;
import com.htet.happystore.repository.UserRepository;
import com.htet.happystore.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository; // 🌟 ထည့်သွင်းထားသည်

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItem>>> getWishlist(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getMyWishlist(getUser(userDetails)), "Wishlist စာရင်း။"));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<String>> toggleWishlist(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long productId) {
        String message = wishlistService.toggleWishlist(getUser(userDetails), productId);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    private User getUser(UserDetails details) {
        return userRepository.findByEmail(details.getUsername())
                .or(() -> userRepository.findByPhone(details.getUsername()))
                .orElseThrow(() -> new IllegalArgumentException("User မတွေ့ပါ"));
    }
}