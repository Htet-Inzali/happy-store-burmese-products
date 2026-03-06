package com.htet.happystore.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalesReportDTO {
    private LocalDateTime orderDate;
    private String productName;
    private Integer quantity;
    private BigDecimal salePriceVND;      // တစ်ခုချင်း ရောင်းဈေး
    private BigDecimal totalSaleVND;      // quantity * salePriceVND
    private BigDecimal costPerItemMMK;    // တစ်ခုချင်း ကုန်ကျစရိတ် (MMK)
    private BigDecimal profitPerItemVND;  // တစ်ခုချင်း အမြတ် (VND)
    private BigDecimal totalProfitVND;    // quantity * profitPerItemVND
}