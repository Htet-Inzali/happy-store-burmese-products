package com.htet.happystore.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExcelProductDTO {
    private String name;
    private Double weightGram;
    private BigDecimal originalPriceMMK;
    private BigDecimal kiloRateMMK;
    private BigDecimal salePriceVND;
    private Integer quantity;
    private LocalDate arrivalDate;  // StockBatch မှာ nullable=false မို့ မဖြစ်မနေ ထည့်ရမယ်
    private LocalDate expiryDate;
}