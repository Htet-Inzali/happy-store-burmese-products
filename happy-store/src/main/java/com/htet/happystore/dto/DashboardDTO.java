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