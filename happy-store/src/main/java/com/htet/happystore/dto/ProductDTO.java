package com.htet.happystore.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ProductDTO {

    @Data
    public static class Request {
        private String name;
        private String description;
        private String imageUrl;
        private Double weightGram;
        private BigDecimal currentPriceVND; // အသစ်ထည့်ထားသည်
        private String sku; // အသစ်ထည့်ထားသည်
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private String imageUrl;
        private Double weightGram;
        private BigDecimal currentPriceVND; // User ပြမည့် တစ်ခုတည်းသော ဈေး
        private Integer totalStock;
        private String sku;
    }

    @Data
    public static class BatchRequest {
        private Long productId;
        private BigDecimal originalPriceMMK;
        private BigDecimal kiloRateMMK;
        private Integer initialQuantity;
        private LocalDate arrivalDate;
        private LocalDate expiryDate;
        private BigDecimal manualSalePriceVND;
    }

    @Data
    public static class ExcelRequest {
        private String name;
        private Double weightGram;
        private BigDecimal originalPriceMMK;
        private BigDecimal kiloRateMMK;
        private BigDecimal salePriceVND;
        private Integer quantity;
        private LocalDate arrivalDate;
        private LocalDate expiryDate;
    }
}