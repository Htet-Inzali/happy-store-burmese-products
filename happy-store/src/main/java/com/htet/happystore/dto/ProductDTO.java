package com.htet.happystore.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ProductDTO {

    // 🌟 Excel bulk upload — preview နှင့် bulk save အတွက် တစ်ကြောင်းချင်း row
    @Data
    public static class BulkRow {
        private String name;
        private Double weightGram;
        private BigDecimal originalPriceMMK;
        private BigDecimal kiloRateMMK;
        private Integer initialQuantity;
        private BigDecimal currentPriceVND; // auto-တွက် ရောင်းဈေး (preview တွင် ပြရန်)
        private LocalDate arrivalDate;
        private LocalDate expiryDate;
        private String imageUrl;            // Excel ထဲ embed ပုံကို Cloudinary တင်ပြီး ရလာသော URL
    }

    @Data
    public static class Request {
        private String name;
        private String description;
        private String imageUrl;
        private Double weightGram;
        private BigDecimal currentPriceVND;
        private String sku;

        // 🌟 First Batch (ပထမဆုံး အသုတ်) အတွက်
        private BigDecimal originalPriceMMK;
        private BigDecimal kiloRateMMK;
        private Integer initialQuantity;
        private LocalDate expiryDate;
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

        private List<BatchResponse> batches;
    }

    // 🌟 Response Class ၏ အပြင်ဘက် (သို့မဟုတ် အောက်ဘက်) တွင် ဤ Class အသစ်ကို ထည့်ပါ
    @Data
    public static class BatchResponse {
        private Long id;
        private Integer remainingQuantity;
        private BigDecimal originalPriceMMK;
        private BigDecimal kiloRateMMK;
        private BigDecimal salePriceVND;
        private LocalDate arrivalDate;
        private LocalDate expiryDate;
    }

    @Data
    public static class BatchRequest {
        private Long productId;
        private BigDecimal originalPriceMMK;
        private BigDecimal kiloRateMMK;
        private Integer initialQuantity;
        private LocalDate arrivalDate;
        private LocalDate expiryDate;
        private BigDecimal newSalePriceVND; // 🌟 ဒီဈေးဖြင့် Product ရဲ့ Live ဈေးကိုပါ Update လုပ်မည်
    }
}