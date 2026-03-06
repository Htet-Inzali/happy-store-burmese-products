// OrderAdminResponse.java
package com.htet.happystore.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderAdminResponse {
    private Long id;
    private String customerName;
    private String customerPhone;
    private LocalDateTime orderDate;
    private BigDecimal totalAmountVND;
    private String status;
    private List<ItemAdminDTO> items;

    @Data
    public static class ItemAdminDTO {
        private String productName;
        private Integer quantity;
        private BigDecimal price;
        private Long batchId; // 🌟 Admin အတွက် Batch ID ထည့်ပေးထားသည်
        private BigDecimal originalCost; // 🌟 မူရင်းရင်းနှီးခုတ်
    }
}