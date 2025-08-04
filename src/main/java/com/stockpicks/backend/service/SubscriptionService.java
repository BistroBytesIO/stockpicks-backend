package com.stockpicks.backend.service;

import com.stockpicks.backend.entity.SubscriptionPlan;
import com.stockpicks.backend.entity.User;
import com.stockpicks.backend.entity.UserSubscription;
import com.stockpicks.backend.enums.SubscriptionStatus;
import com.stockpicks.backend.repository.SubscriptionPlanRepository;
import com.stockpicks.backend.repository.UserSubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    @Autowired
    private StripeService stripeService;

    @Autowired
    private UserService userService;

    public List<SubscriptionPlan> getAllActivePlans() {
        return subscriptionPlanRepository.findByIsActiveTrue();
    }

    public UserSubscription createSubscription(String email, Long planId) throws StripeException {
        User user = userService.findByEmail(email);
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        // Check if user already has an active subscription
        if (userSubscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)) {
            throw new RuntimeException("User already has an active subscription");
        }

        // Create Stripe customer if not exists
        String customerName = user.getFirstName() + " " + user.getLastName();
        String customerId = stripeService.createCustomer(user.getEmail(), customerName);

        // Create Stripe subscription with the plan's Stripe price ID
        String stripePriceId = plan.getStripePriceId();
        if (stripePriceId == null || stripePriceId.isEmpty()) {
            throw new RuntimeException("Subscription plan does not have a valid Stripe price ID");
        }
        Subscription stripeSubscription = stripeService.createSubscription(customerId, stripePriceId);

        // Create user subscription record
        UserSubscription userSubscription = new UserSubscription();
        userSubscription.setUser(user);
        userSubscription.setPlan(plan);
        userSubscription.setStripeSubscriptionId(stripeSubscription.getId());
        userSubscription.setStatus(SubscriptionStatus.valueOf(stripeSubscription.getStatus().toUpperCase()));
        
        if (stripeSubscription.getCurrentPeriodStart() != null) {
            userSubscription.setCurrentPeriodStart(
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodStart()), ZoneId.systemDefault())
            );
        }
        
        if (stripeSubscription.getCurrentPeriodEnd() != null) {
            userSubscription.setCurrentPeriodEnd(
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()), ZoneId.systemDefault())
            );
        }

        return userSubscriptionRepository.save(userSubscription);
    }

    public void cancelSubscription(String email) throws StripeException {
        User user = userService.findByEmail(email);
        UserSubscription userSubscription = userSubscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription found"));

        // Cancel in Stripe
        stripeService.cancelSubscription(userSubscription.getStripeSubscriptionId());

        // Update local record
        userSubscription.setStatus(SubscriptionStatus.CANCELED);
        userSubscriptionRepository.save(userSubscription);
    }

    public UserSubscription getCurrentSubscription(String email) {
        User user = userService.findByEmail(email);
        return userSubscriptionRepository.findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .orElse(null);
    }

    public boolean hasActiveSubscription(String email) {
        User user = userService.findByEmail(email);
        return userSubscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE);
    }

    public void updateSubscriptionStatus(String stripeSubscriptionId, SubscriptionStatus status) {
        UserSubscription userSubscription = userSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        userSubscription.setStatus(status);
        userSubscriptionRepository.save(userSubscription);
    }
    
    public UserSubscription createSubscriptionFromCheckout(String email, Long planId, String stripeSubscriptionId) throws StripeException {
        System.out.println("Creating subscription from checkout - Email: " + email + ", PlanId: " + planId + ", StripeSubId: " + stripeSubscriptionId);
        
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found: " + email);
        }
        System.out.println("Found user: " + user.getEmail());
        
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + planId));
        System.out.println("Found plan: " + plan.getName());

        // Check if user already has an active subscription
        if (userSubscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)) {
            System.out.println("User already has active subscription, skipping creation");
            throw new RuntimeException("User already has an active subscription");
        }

        // Get the Stripe subscription details
        System.out.println("Retrieving Stripe subscription: " + stripeSubscriptionId);
        Subscription stripeSubscription = stripeService.retrieveSubscription(stripeSubscriptionId);
        System.out.println("Retrieved Stripe subscription status: " + stripeSubscription.getStatus());

        // Create user subscription record
        UserSubscription userSubscription = new UserSubscription();
        userSubscription.setUser(user);
        userSubscription.setPlan(plan);
        userSubscription.setStripeSubscriptionId(stripeSubscription.getId());
        userSubscription.setStatus(SubscriptionStatus.valueOf(stripeSubscription.getStatus().toUpperCase()));
        
        if (stripeSubscription.getCurrentPeriodStart() != null) {
            userSubscription.setCurrentPeriodStart(
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodStart()), ZoneId.systemDefault())
            );
        }
        
        if (stripeSubscription.getCurrentPeriodEnd() != null) {
            userSubscription.setCurrentPeriodEnd(
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()), ZoneId.systemDefault())
            );
        }

        UserSubscription savedSubscription = userSubscriptionRepository.save(userSubscription);
        System.out.println("Successfully saved subscription with ID: " + savedSubscription.getId());
        return savedSubscription;
    }
}