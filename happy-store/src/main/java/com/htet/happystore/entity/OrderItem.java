package com.htet.happystore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private StockBatch batch; // Admin Profit တွက်ရန် မရှိမဖြစ်လိုအပ်

    @Min(1)
    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal priceAtPurchaseVND; // ဝယ်ချိန်က ဈေးနှုန်း snapshot (Product ထဲက currentPrice ကို ကူးထည့်ရန်)
}