package com.stockpicks.backend.controller;

import com.stockpicks.backend.enums.SubscriptionStatus;
import com.stockpicks.backend.service.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, 
                                                     @RequestHeader("Stripe-Signature") String sigHeader) {
        
        Event event;
        
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            System.err.println("Invalid signature: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "customer.subscription.created":
                    handleSubscriptionCreated(event);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                default:
                    System.out.println("Unhandled event type: " + event.getType());
            }
        } catch (Exception e) {
            System.err.println("Error handling webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }

        return ResponseEntity.ok("Webhook processed successfully");
    }

    private void handleCheckoutSessionCompleted(Event event) throws StripeException {
        Session session = (Session) event.getData().getObject();
        System.out.println("Checkout session completed: " + session.getId());
        System.out.println("Session mode: " + session.getMode());
        System.out.println("Customer email: " + session.getCustomerEmail());
        System.out.println("Session metadata: " + session.getMetadata());
        System.out.println("Subscription ID: " + session.getSubscription());
        
        if (session.getMode().equals("subscription")) {
            String planIdStr = session.getMetadata().get("plan_id");
            if (planIdStr != null) {
                Long planId = Long.parseLong(planIdStr);
                String customerEmail = session.getCustomerEmail();
                
                System.out.println("Creating subscription for email: " + customerEmail + ", planId: " + planId);
                
                try {
                    // Create the subscription in our database
                    subscriptionService.createSubscriptionFromCheckout(customerEmail, planId, session.getSubscription());
                    System.out.println("Successfully created subscription in database");
                } catch (Exception e) {
                    System.err.println("Error creating subscription from checkout: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            } else {
                System.err.println("No plan_id found in session metadata");
            }
        } else {
            System.out.println("Session mode is not subscription: " + session.getMode());
        }
    }

    private void handleSubscriptionCreated(Event event) throws StripeException {
        Subscription subscription = (Subscription) event.getData().getObject();
        System.out.println("Subscription created: " + subscription.getId());
        
        try {
            subscriptionService.createOrUpdateSubscriptionFromStripe(
                subscription.getId(), 
                SubscriptionStatus.valueOf(subscription.getStatus().toUpperCase())
            );
            System.out.println("Successfully processed subscription created event");
        } catch (Exception e) {
            System.err.println("Error processing subscription created: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void handleSubscriptionUpdated(Event event) throws StripeException {
        Subscription subscription = (Subscription) event.getData().getObject();
        System.out.println("Subscription updated: " + subscription.getId());
        
        try {
            subscriptionService.createOrUpdateSubscriptionFromStripe(
                subscription.getId(), 
                SubscriptionStatus.valueOf(subscription.getStatus().toUpperCase())
            );
            System.out.println("Successfully processed subscription updated event");
        } catch (Exception e) {
            System.err.println("Error processing subscription updated: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void handleSubscriptionDeleted(Event event) throws StripeException {
        Subscription subscription = (Subscription) event.getData().getObject();
        System.out.println("Subscription deleted: " + subscription.getId());
        
        try {
            subscriptionService.createOrUpdateSubscriptionFromStripe(
                subscription.getId(), 
                SubscriptionStatus.CANCELED
            );
            System.out.println("Successfully processed subscription deleted event");
        } catch (Exception e) {
            System.err.println("Error processing subscription deleted: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        System.out.println("Invoice payment succeeded");
    }

    private void handleInvoicePaymentFailed(Event event) {
        System.out.println("Invoice payment failed");
    }
}