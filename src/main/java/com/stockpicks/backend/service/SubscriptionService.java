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
import org.springframework.transaction.annotation.Transactional;

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
        if (userSubscriptionRepository.existsByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE)) {
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
        userSubscription.setUserId(user.getId());
        userSubscription.setPlanId(plan.getId());
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
        UserSubscription userSubscription = userSubscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription found"));

        // Cancel in Stripe
        stripeService.cancelSubscription(userSubscription.getStripeSubscriptionId());

        // Update local record
        userSubscription.setStatus(SubscriptionStatus.CANCELED);
        userSubscriptionRepository.save(userSubscription);
    }

    @Transactional
    public UserSubscription getCurrentSubscription(String email) {
        User user = userService.findByEmail(email);
        
        // Use the ordered query to get active subscriptions by most recent update
        List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), SubscriptionStatus.ACTIVE);
        
        if (activeSubscriptions.isEmpty()) {
            return null;
        }
        
        // If there are multiple active subscriptions (which shouldn't happen after our fix),
        // return the most recently updated one and log a warning
        if (activeSubscriptions.size() > 1) {
            System.err.println("WARNING: User " + email + " has " + activeSubscriptions.size() + " active subscriptions. Using the most recent one.");
            
            // Cancel all but the most recent active subscription
            for (int i = 1; i < activeSubscriptions.size(); i++) {
                UserSubscription oldSubscription = activeSubscriptions.get(i);
                System.err.println("Automatically cancelling duplicate active subscription: " + oldSubscription.getStripeSubscriptionId());
                oldSubscription.setStatus(SubscriptionStatus.CANCELED);
                oldSubscription.setUpdatedAt(LocalDateTime.now());
                userSubscriptionRepository.save(oldSubscription);
            }
        }
        
        return activeSubscriptions.get(0);
    }

    public boolean hasActiveSubscription(String email) {
        User user = userService.findByEmail(email);
        return userSubscriptionRepository.existsByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE);
    }

    public void updateSubscriptionStatus(String stripeSubscriptionId, SubscriptionStatus status) {
        UserSubscription userSubscription = userSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        
        userSubscription.setStatus(status);
        userSubscriptionRepository.save(userSubscription);
    }
    
    @Transactional
    public UserSubscription createOrUpdateSubscriptionFromStripe(String stripeSubscriptionId, SubscriptionStatus status) throws StripeException {
        System.out.println("Creating or updating subscription from Stripe: " + stripeSubscriptionId + " with status: " + status);
        
        // First try to find existing subscription by Stripe ID
        UserSubscription existingSubscription = userSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElse(null);
        
        if (existingSubscription != null) {
            System.out.println("Found existing subscription, updating status from " + existingSubscription.getStatus() + " to " + status);
            existingSubscription.setStatus(status);
            existingSubscription.setUpdatedAt(LocalDateTime.now());
            return userSubscriptionRepository.save(existingSubscription);
        }
        
        // Subscription doesn't exist, we need to create it
        System.out.println("Subscription not found in DB, creating new subscription record");
        
        // Get subscription details from Stripe
        Subscription stripeSubscription = stripeService.retrieveSubscription(stripeSubscriptionId);
        System.out.println("Retrieved Stripe subscription: " + stripeSubscription.getId());
        
        // Get customer email from Stripe
        String customerEmail = stripeService.getCustomerEmailById(stripeSubscription.getCustomer());
        System.out.println("Customer email: " + customerEmail);
        
        // Find user by email
        User user = userService.findByEmail(customerEmail);
        if (user == null) {
            throw new RuntimeException("User not found for email: " + customerEmail);
        }
        System.out.println("Found user: " + user.getEmail());
        
        // Check if user already has an active subscription to prevent duplicates
        if (status == SubscriptionStatus.ACTIVE && userSubscriptionRepository.existsByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE)) {
            System.out.println("User already has an active subscription, cancelling old active subscriptions");
            // Find and cancel existing active subscriptions to prevent duplicates
            UserSubscription existingActive = userSubscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE).orElse(null);
            if (existingActive != null && !existingActive.getStripeSubscriptionId().equals(stripeSubscriptionId)) {
                System.out.println("Cancelling existing active subscription: " + existingActive.getStripeSubscriptionId());
                existingActive.setStatus(SubscriptionStatus.CANCELED);
                existingActive.setUpdatedAt(LocalDateTime.now());
                userSubscriptionRepository.save(existingActive);
            }
        }
        
        // Get the subscription plan by matching the Stripe price ID
        String stripePriceId = stripeSubscription.getItems().getData().get(0).getPrice().getId();
        System.out.println("Stripe Price ID: " + stripePriceId);
        
        SubscriptionPlan plan = subscriptionPlanRepository.findByStripePriceId(stripePriceId)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found for Stripe price ID: " + stripePriceId));
        System.out.println("Found plan: " + plan.getName());
        
        // Create user subscription record
        UserSubscription userSubscription = new UserSubscription();
        userSubscription.setUserId(user.getId());
        userSubscription.setPlanId(plan.getId());
        userSubscription.setStripeSubscriptionId(stripeSubscription.getId());
        userSubscription.setStatus(status);
        
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
        System.out.println("Successfully created subscription with ID: " + savedSubscription.getId());
        return savedSubscription;
    }
}