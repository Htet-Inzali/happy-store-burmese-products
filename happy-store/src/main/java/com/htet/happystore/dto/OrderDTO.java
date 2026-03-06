package com.htet.happystore.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDTO {

    @Data
    public static class Request {
        @NotBlank(message = "Delivery type is required")
        private String deliveryType; // COD or PICKUP

        @NotBlank(message = "Shipping address is required")
        private String shippingAddress;

        private String contactPhone;

        @NotEmpty(message = "Order must have at least one item")
        private List<CartItem> items;

        @Data
        public static class CartItem {
            @NotNull(message = "Product ID is required")
            private Long productId;

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            private Integer quantity;
        }
    }

    @Data
    public static class UserResponse {
        private Long id;
        private LocalDateTime orderDate;
        private BigDecimal totalAmountVND;
        private String status;
        private String deliveryType;
        private List<Item> items;

        @Data
        public static class Item {
            private String productName;
            private Integer quantity;
            private BigDecimal price; // priceAtPurchaseVND
        }
    }

    @Data
    public static class AdminResponse {
        private Long id;
        private String customerName;
        private String customerPhone;
        private LocalDateTime orderDate;
        private BigDecimal totalAmountVND;
        private String status;
        private List<Item> items;

        @Data
        public static class Item {
            private String productName;
            private Integer quantity;
            private BigDecimal price; // User ဝယ်သွားတဲ့ဈေး
            private Long batchId;     // အမြတ်တွက်ရန် Batch ID
            private BigDecimal originalCost;
        }
    }
}