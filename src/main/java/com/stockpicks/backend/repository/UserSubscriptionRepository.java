package com.stockpicks.backend.repository;

import com.stockpicks.backend.entity.User;
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
    List<UserSubscription> findByUser(User user);
    Optional<UserSubscription> findByUserAndStatus(User user, SubscriptionStatus status);
    List<UserSubscription> findByUserAndStatusOrderByUpdatedAtDesc(User user, SubscriptionStatus status);
    Optional<UserSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    boolean existsByUserAndStatus(User user, SubscriptionStatus status);
    
    @Query("SELECT us FROM UserSubscription us WHERE us.user = :user AND us.status = :status ORDER BY us.updatedAt DESC")
    List<UserSubscription> findActiveSubscriptionsOrderedByUpdateTime(@Param("user") User user, @Param("status") SubscriptionStatus status);
}