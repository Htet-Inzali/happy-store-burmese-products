package com.htet.happystore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal originalPriceMMK;   // မြန်မာဝယ်ရင်းဈေး

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal kiloRateMMK;        // သယ်ယူခ (1kg နှုန်း)

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal salePriceVND;       // Admin သတ်မှတ်တဲ့ ရောင်းဈေး

    @Column(nullable = false)
    private Integer initialQuantity;       // အစောပိုင်းအဝင်အရေအတွက်

    @Column(nullable = false)
    private Integer remainingQuantity;     // လက်ကျန်အရေအတွက်

    @Column(nullable = false)
    private LocalDate arrivalDate;         // ပစ္စည်းရောက်သည့်နေ့

    private LocalDate expiryDate;          // သက်တမ်းကုန်ဆုံးရက်

    @Version
    private Long version;

    // ==============================
    // Data Integrity Validation
    // ==============================

    @PrePersist
    @PreUpdate
    private void validate() {

        if (initialQuantity == null || initialQuantity < 0) {
            throw new IllegalArgumentException("Initial quantity must be zero or positive");
        }

        if (remainingQuantity == null || remainingQuantity < 0) {
            throw new IllegalArgumentException("Remaining quantity must be zero or positive");
        }

        if (remainingQuantity > initialQuantity) {
            throw new IllegalArgumentException("Remaining quantity cannot exceed initial quantity");
        }

        if (originalPriceMMK == null || originalPriceMMK.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Original price must be zero or positive");
        }

        if (kiloRateMMK == null || kiloRateMMK.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Kilo rate must be zero or positive");
        }

        if (salePriceVND == null || salePriceVND.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Sale price must be zero or positive");
        }

        if (arrivalDate == null) {
            throw new IllegalArgumentException("Arrival date is required");
        }
    }

    // ==============================
    // Business Logic
    // ==============================

    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }

    public boolean isEmpty() {
        return remainingQuantity != null && remainingQuantity == 0;
    }

    // Kilo cost calculation
    public BigDecimal getCalculatedKiloCost() {

        if (product == null
                || product.getWeightGram() == null
                || kiloRateMMK == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal weightInKg = BigDecimal
                .valueOf(product.getWeightGram())
                .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);

        return weightInKg
                .multiply(kiloRateMMK)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Total cost in MMK
    public BigDecimal getTotalCostMMK() {

        BigDecimal original = originalPriceMMK != null
                ? originalPriceMMK
                : BigDecimal.ZERO;

        return original
                .add(getCalculatedKiloCost())
                .setScale(2, RoundingMode.HALF_UP);
    }
}