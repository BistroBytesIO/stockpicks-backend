package com.stockpicks.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer durationMonths;

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Constructor
    public SubscriptionPlan(String name, String description, BigDecimal price, Integer durationMonths, String stripePriceId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationMonths = durationMonths;
        this.stripePriceId = stripePriceId;
    }
}