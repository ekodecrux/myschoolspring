package com.myschool.backend.controller;

import com.myschool.backend.models.entity.User;
import com.myschool.backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rest/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

        // Get available subscription plans and credit packs
    @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> getPlans() {
        Map<String, Object> result = paymentService.getPlans();
        return ResponseEntity.ok(result);
    }

        // Create a Razorpay order for payment
    @PostMapping("/razorpay/create-order")
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = paymentService.createRazorpayOrder(body, currentUser);
        return ResponseEntity.ok(result);
    }

        // Verify Razorpay payment and add credits
    @PostMapping("/razorpay/verify")
    public ResponseEntity<Map<String, Object>> verifyRazorpayPayment(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = paymentService.verifyRazorpayPayment(body, currentUser);
        return ResponseEntity.ok(result);
    }

        // Create a Stripe checkout session
    @PostMapping("/stripe/create-session")
    public ResponseEntity<Map<String, Object>> createStripeSession(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = paymentService.createStripeSession(body, currentUser);
        return ResponseEntity.ok(result);
    }

        // Stripe webhook handler
    @PostMapping("/stripe/webhook")
    public ResponseEntity<Map<String, Object>> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        Map<String, Object> result = paymentService.stripeWebhook(payload, sigHeader);
        return ResponseEntity.ok(result);
    }

        // Get user's order/payment history
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getOrderHistory(@AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = paymentService.getOrderHistory(currentUser);
        return ResponseEntity.ok(result);
    }

        // Add credits to a user (Super Admin only)
    @PostMapping("/admin/add-credits")
    public ResponseEntity<Map<String, Object>> addCreditsAdmin(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = paymentService.addCreditsAdmin(body, currentUser);
        return ResponseEntity.ok(result);
    }
}
