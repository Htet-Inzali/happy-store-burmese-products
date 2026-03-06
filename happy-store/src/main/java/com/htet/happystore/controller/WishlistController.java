package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.entity.User;
import com.htet.happystore.entity.WishlistItem;
import com.htet.happystore.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItem>>> getWishlist(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getMyWishlist(user), "Wishlist စာရင်း။"));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<String>> toggleWishlist(@AuthenticationPrincipal User user, @PathVariable Long productId) {
        String message = wishlistService.toggleWishlist(user, productId);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }
}
