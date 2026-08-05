package com.htet.happystore.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CustomerDTO {

    @Data
    public static class Summary {
        private Long id;
        private String fullName;
        private String phone;
        private String email;
        private String country;
        private long orderCount;          // ပယ်ဖျက်မဟုတ်သော order အရေအတွက်
        private BigDecimal totalSpentVND;  // စုစုပေါင်း သုံးစွဲငွေ (ပယ်ဖျက်မပါ)
        private LocalDateTime lastOrderDate;
    }
}
