package com.htet.happystore.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

public class DashboardDTO {

    @Data
    public static class Summary {
        private BigDecimal todayRevenue;
        private BigDecimal todayProfit;
        private long newOrdersCount;
        private long pendingPreordersCount;
        private long lowStockProductsCount;
        private long expiringBatchesCount;

        // 🌟 အသစ် ထပ်တိုး metrics
        private long totalOrdersCount;           // ကာလအတွင်း order အရေအတွက် (cancelled မပါ)
        private long totalItemsSold;             // ကာလအတွင်း ရောင်းခဲ့သော ပစ္စည်း အရေအတွက်
        private BigDecimal averageOrderValueVND;  // ပျမ်းမျှ order တန်ဖိုး
        private BigDecimal onlineRevenue;         // Online ရောင်းအား
        private BigDecimal walkInRevenue;         // ဆိုင်ရှေ့ (POS) ရောင်းအား
        private BigDecimal profitMarginPercent;   // အမြတ် % (profit / revenue * 100)
        private BigDecimal inventoryValueVND;     // လက်ကျန် stock ၏ ကုန်ကျစရိတ် တန်ဖိုး
        private long totalActiveProducts;         // ရောင်းနေသော ပစ္စည်းမျိုးစုံ အရေအတွက်
    }

    @Data
    public static class SalesTrendPoint {
        private LocalDate date;
        private BigDecimal revenue;
        private BigDecimal profit;
        private long orders;
    }

    @Data
    public static class TopProduct {
        private String name;
        private long totalSold;
    }

    @Data
    public static class ExpiringBatch {
        private String productName;
        private String sku;
        private int remainingQuantity;
        private LocalDate expiryDate;
    }
}