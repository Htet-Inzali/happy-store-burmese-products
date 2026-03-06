package com.htet.happystore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LowStockDTO {
    private String productName;
    private Long remainingStock;
}