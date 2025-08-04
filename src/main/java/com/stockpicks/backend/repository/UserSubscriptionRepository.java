package com.stockpicks.backend.repository;

import com.stockpicks.backend.entity.User;
import com.stockpicks.backend.entity.UserSubscription;
import com.stockpicks.backend.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    List<UserSubscription> findByUser(User user);
    Optional<UserSubscription> findByUserAndStatus(User user, SubscriptionStatus status);
    Optional<UserSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    boolean existsByUserAndStatus(User user, SubscriptionStatus status);
}