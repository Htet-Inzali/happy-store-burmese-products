package com.htet.happystore.repository;

import com.htet.happystore.entity.Product;
import com.htet.happystore.entity.User;
import com.htet.happystore.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUserOrderByAddedDateDesc(User user);
    Optional<WishlistItem> findByUserAndProduct(User user, Product product);
}