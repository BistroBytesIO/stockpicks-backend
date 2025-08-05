package com.stockpicks.backend.repository;

import com.stockpicks.backend.entity.UserSubscription;
import com.stockpicks.backend.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    List<UserSubscription> findByUserId(Long userId);
    Optional<UserSubscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);
    List<UserSubscription> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, SubscriptionStatus status);
    Optional<UserSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    boolean existsByUserIdAndStatus(Long userId, SubscriptionStatus status);
    
    @Query("SELECT us FROM UserSubscription us WHERE us.userId = :userId AND us.status = :status ORDER BY us.updatedAt DESC")
    List<UserSubscription> findActiveSubscriptionsOrderedByUpdateTime(@Param("userId") Long userId, @Param("status") SubscriptionStatus status);
}