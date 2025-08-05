package com.stockpicks.backend.entity;

import com.stockpicks.backend.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscriptions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "plan_id")
    private Long planId;

    private String stripeSubscriptionId;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Constructors
    public UserSubscription(Long userId, Long planId, String stripeSubscriptionId, SubscriptionStatus status) {
        this.userId = userId;
        this.planId = planId;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.status = status;
    }
}