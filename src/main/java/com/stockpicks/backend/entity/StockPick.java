package com.stockpicks.backend.entity;

import com.stockpicks.backend.enums.PickType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_picks")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockPick {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    private String companyName;

    @Enumerated(EnumType.STRING)
    private PickType pickType;

    @Column(precision = 10, scale = 2)
    private BigDecimal entryPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal targetPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal stopLoss;

    @Column(columnDefinition = "TEXT")
    private String commentary;

    @Column(nullable = false)
    private LocalDateTime pickDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private Boolean isActive = true;

    private LocalDateTime updatedAt;

    private BigDecimal currentPrice;
}