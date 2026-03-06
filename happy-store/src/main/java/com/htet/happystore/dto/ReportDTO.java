package com.htet.happystore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReportDTO {

    @Data
    public static class Sales {
        private LocalDateTime orderDate;
        private String productName;
        private Integer quantity;
        private BigDecimal salePriceVND;
        private BigDecimal totalSaleVND;
        private BigDecimal costPerItemMMK;
        private BigDecimal profitPerItemVND;
        private BigDecimal totalProfitVND;
    }

    @Data
    @AllArgsConstructor
    public static class TopProduct {
        private String productName;
        private Long totalSold;
    }

    @Data
    @AllArgsConstructor
    public static class LowStock {
        private String productName;
        private Long remainingStock;
    }
}