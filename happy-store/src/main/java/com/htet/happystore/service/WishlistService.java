package com.htet.happystore.service;

import com.htet.happystore.entity.Product;
import com.htet.happystore.entity.User;
import com.htet.happystore.entity.WishlistItem;
import com.htet.happystore.repository.ProductRepository;
import com.htet.happystore.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    @Transactional
    public String toggleWishlist(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product ရှာမတွေ့ပါ"));

        Optional<WishlistItem> existingItem = wishlistRepository.findByUserAndProduct(user, product);

        if (existingItem.isPresent()) {
            wishlistRepository.delete(existingItem.get());
            return "Wishlist ထဲမှ ပြန်ဖြုတ်လိုက်ပါပြီ။";
        } else {
            WishlistItem newItem = new WishlistItem();
            newItem.setUser(user);
            newItem.setProduct(product);
            wishlistRepository.save(newItem);
            return "Wishlist ထဲသို့ ထည့်သွင်းလိုက်ပါပြီ။";
        }
    }

    public List<WishlistItem> getMyWishlist(User user) {
        return wishlistRepository.findByUserOrderByAddedDateDesc(user);
    }
}