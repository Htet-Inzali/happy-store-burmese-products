package com.htet.happystore.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items")
@Data
public class WishlistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // 🌟 ဘယ် User လဲ

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product; // 🌟 ဘယ်ပစ္စည်းလဲ

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime addedDate;
}