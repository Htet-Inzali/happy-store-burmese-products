package com.htet.happystore.controller;

import com.htet.happystore.dto.ApiResponse;
import com.htet.happystore.entity.User;
import com.htet.happystore.repository.UserRepository;
import com.htet.happystore.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository; // 🌟 ထည့်သွင်းထားသည်

    // WishlistController.java ၏ getWishlist နေရာတွင် အစားထိုးရန်
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWishlist(@AuthenticationPrincipal UserDetails userDetails) {
        List<Map<String, Object>> responseList = wishlistService.getMyWishlist(getUser(userDetails))
                .stream()
                .map(item -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", item.getId());
                    map.put("addedDate", item.getAddedDate());
                    if(item.getProduct() != null) {
                        map.put("productId", item.getProduct().getId());
                        map.put("productName", item.getProduct().getName());
                        map.put("productImage", item.getProduct().getImageUrl());
                        map.put("productPriceVND", item.getProduct().getCurrentPriceVND());
                    }
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responseList, "Wishlist စာရင်း။"));
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