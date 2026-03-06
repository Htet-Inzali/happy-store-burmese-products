package com.htet.happystore.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BatchRequest {
    private Long productId; // 🌟 ဘယ်ပစ္စည်းအတွက် Stock ဖြည့်မှာလဲ
    private BigDecimal originalPriceMMK;
    private BigDecimal kiloRateMMK;
    private Integer initialQuantity;
    private LocalDate arrivalDate;
    private LocalDate expiryDate;
    private BigDecimal manualSalePriceVND; // 🌟 Admin က ကိုယ်တိုင်ပေးချင်တဲ့ဈေး

}