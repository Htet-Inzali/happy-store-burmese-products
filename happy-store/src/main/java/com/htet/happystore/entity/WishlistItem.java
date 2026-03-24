// WishlistItem.java တွင် ပြင်ရန်
package com.htet.happystore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items")
@Getter @Setter  // 🌟 @Data အစား ပြောင်းသုံးပါ
public class WishlistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 🌟 LAZY ပါ တစ်ခါတည်း ထည့်ပေးပါ
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) // 🌟 LAZY ပါ တစ်ခါတည်း ထည့်ပေးပါ
    @JoinColumn(name = "product_id")
    private Product product;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime addedDate;
}