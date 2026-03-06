package com.htet.happystore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private Double weightGram; // e.g. 140.0 g

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal currentPriceVND; // User မြင်ရမည့် တစ်ခုတည်းသော Live ရောင်းဈေး

    private String imageUrl;

    private String sku;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private boolean isActive = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<StockBatch> batches;
}