package com.htet.happystore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @NotBlank(message = "Delivery type is required")
    private String deliveryType; // COD or PICKUP

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    private String contactPhone;

    @NotEmpty(message = "Order must have at least one item")
    private List<CartItemDTO> items;

    @Data
    public static class CartItemDTO {

        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}