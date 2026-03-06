package com.htet.happystore.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private Double weightGram;
    private String category;
    private BigDecimal salePriceVND; // အစောဆုံး available batch ရဲ့ ဈေး
    private Integer totalStock;      // Batch အားလုံး ပေါင်း လက်ကျန်
    private String imageUrl;
}