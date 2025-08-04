package com.stockpicks.backend.dto.subscription;

import com.stockpicks.backend.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResponse {
    private Long id;
    private String planName;
    private SubscriptionStatus status;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private String checkoutUrl;

    public SubscriptionResponse(Long id, String planName, SubscriptionStatus status, LocalDateTime currentPeriodStart, LocalDateTime currentPeriodEnd) {
        this.id = id;
        this.planName = planName;
        this.status = status;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
    }
}