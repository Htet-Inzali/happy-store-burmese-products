package com.htet.happystore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String settingKey; // e.g. "MMK_VND_RATE"

    @Column(precision = 19, scale = 6, nullable = false)
    private BigDecimal settingValue; // e.g. 8.5

    private String description;
}