// OrderUserResponse.java
package com.htet.happystore.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderUserResponse {
    private Long id;
    private LocalDateTime orderDate;
    private BigDecimal totalAmountVND;
    private String status;
    private String deliveryType;
    private List<ItemUserDTO> items;

    @Data
    public static class ItemUserDTO {
        private String productName;
        private Integer quantity;
        private BigDecimal price;
    }
}