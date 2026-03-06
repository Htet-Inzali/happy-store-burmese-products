package com.htet.happystore.dto;

import lombok.Data;

@Data
public class ProductRequest {
    private String name;
    private String description;
    private String category;
    private String imageUrl;
    private Double weightGram;
}