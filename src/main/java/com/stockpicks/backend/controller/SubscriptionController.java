package com.stockpicks.backend.controller;

import com.stockpicks.backend.dto.subscription.CheckoutSessionResponse;
import com.stockpicks.backend.dto.subscription.CreateSubscriptionRequest;
import com.stockpicks.backend.dto.subscription.SubscriptionResponse;
import com.stockpicks.backend.service.StripeService;
import com.stockpicks.backend.entity.SubscriptionPlan;
import com.stockpicks.backend.entity.User;
import com.stockpicks.backend.entity.UserSubscription;
import com.stockpicks.backend.service.SubscriptionService;
import com.stockpicks.backend.service.UserService;
import com.stockpicks.backend.repository.SubscriptionPlanRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private StripeService stripeService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getAllActivePlans() {
        List<SubscriptionPlan> plans = subscriptionService.getAllActivePlans();
        return ResponseEntity.ok(plans);
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@Valid @RequestBody CreateSubscriptionRequest request, Authentication authentication) {
        try {
            String email = authentication.getName();
            
            // Get the subscription plan
            SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Subscription plan not found"));
            
            if (plan.getStripePriceId() == null || plan.getStripePriceId().isEmpty()) {
                return ResponseEntity.badRequest().body("Subscription plan does not have a valid Stripe price ID");
            }
            
            // Check if user already has an active subscription
            if (subscriptionService.hasActiveSubscription(email)) {
                return ResponseEntity.badRequest().body("User already has an active subscription");
            }
            
            // Create Stripe checkout session
            Session session = stripeService.createCheckoutSession(email, plan.getStripePriceId(), plan.getId());
            
            return ResponseEntity.ok(new CheckoutSessionResponse(session.getUrl()));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Error creating checkout session: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/create")
    public ResponseEntity<?> createSubscription(@Valid @RequestBody CreateSubscriptionRequest request, Authentication authentication) {
        try {
            String email = authentication.getName();
            UserSubscription subscription = subscriptionService.createSubscription(email, request.getPlanId());
            
            // Get plan name using planId
            String planName = "Unknown Plan";
            if (subscription.getPlanId() != null) {
                SubscriptionPlan plan = subscriptionPlanRepository.findById(subscription.getPlanId())
                        .orElse(null);
                if (plan != null) {
                    planName = plan.getName();
                }
            }
            
            SubscriptionResponse response = new SubscriptionResponse(
                subscription.getId(),
                planName,
                subscription.getStatus(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd()
            );
            
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Error creating subscription: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSubscription(Authentication authentication) {
        try {
            String email = authentication.getName();
            subscriptionService.cancelSubscription(email);
            return ResponseEntity.ok("Subscription canceled successfully");
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Error canceling subscription: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentSubscription(Authentication authentication) {
        String email = authentication.getName();
        UserSubscription subscription = subscriptionService.getCurrentSubscription(email);
        
        if (subscription == null) {
            return ResponseEntity.ok().body("No active subscription found");
        }
        
        // Get plan name using planId
        String planName = "Unknown Plan";
        if (subscription.getPlanId() != null) {
            SubscriptionPlan plan = subscriptionPlanRepository.findById(subscription.getPlanId())
                    .orElse(null);
            if (plan != null) {
                planName = plan.getName();
            }
        }
        
        SubscriptionResponse response = new SubscriptionResponse(
            subscription.getId(),
            planName,
            subscription.getStatus(),
            subscription.getCurrentPeriodStart(),
            subscription.getCurrentPeriodEnd()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Boolean> hasActiveSubscription(Authentication authentication) {
        String email = authentication.getName();
        boolean hasActive = subscriptionService.hasActiveSubscription(email);
        return ResponseEntity.ok(hasActive);
    }
}