package com.stockpicks.backend.dto.user;

import com.stockpicks.backend.dto.subscription.SubscriptionResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserWithSubscriptionResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
    private List<SubscriptionResponse> subscriptions;
}