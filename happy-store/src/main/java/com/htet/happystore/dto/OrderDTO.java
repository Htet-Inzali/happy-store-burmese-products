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

    // ==========================================
    // Walk-in (ဆိုင်ရှေ့) ရောင်းအား — Admin သုံး
    // ==========================================
    @Data
    public static class WalkInRequest {
        @NotEmpty(message = "ရောင်းမည့် ပစ္စည်း အနည်းဆုံး တစ်ခု ရှိရမည်")
        private List<Item> items;

        private String customerName;  // optional — မထည့်ရင် "Walk-in Customer"
        private String note;          // optional — မှတ်ချက်

        @Data
        public static class Item {
            @NotNull(message = "Product ID is required")
            private Long productId;

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            private Integer quantity;

            // optional — မထည့်ရင် product ၏ လက်ရှိ ရောင်းဈေး (currentPriceVND) သုံးမည်
            private BigDecimal priceVND;
        }
    }

    @Data
    public static class UserResponse {
        private Long id;
        // UserResponse နှင့် AdminResponse နှစ်ခုလုံးထဲတွင် ထည့်ရန်
        private String orderNumber;
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
        // UserResponse နှင့် AdminResponse နှစ်ခုလုံးထဲတွင် ထည့်ရန်
        private String orderNumber;
        private String customerName;
        private String customerPhone;
        private LocalDateTime orderDate;
        private BigDecimal totalAmountVND;
        private String status;
        private String paymentStatus; // UNPAID / PAID
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