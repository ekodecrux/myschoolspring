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

    @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> getPlans() {
        return ResponseEntity.ok(paymentService.getPlans());
    }

    @PostMapping("/razorpay/create-order")
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(body, currentUser));
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<Map<String, Object>> verifyRazorpayPayment(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(paymentService.verifyRazorpayPayment(body, currentUser));
    }

    // Alias matching original FastAPI route name
    @PostMapping("/razorpay/verify-payment")
    public ResponseEntity<Map<String, Object>> verifyRazorpayPaymentAlias(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(paymentService.verifyRazorpayPayment(body, currentUser));
    }

    @PostMapping("/stripe/create-session")
    public ResponseEntity<Map<String, Object>> createStripeSession(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(paymentService.createStripeSession(body, currentUser));
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<Map<String, Object>> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        return ResponseEntity.ok(paymentService.stripeWebhook(payload, sigHeader));
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getOrderHistory(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(paymentService.getOrderHistory(currentUser));
    }

    // Returns current user's credit balance (matches FastAPI /user/credits)
    @GetMapping("/user/credits")
    public ResponseEntity<Map<String, Object>> getUserCredits(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(paymentService.getUserCredits(currentUser));
    }

    @PostMapping("/admin/add-credits")
    public ResponseEntity<Map<String, Object>> addCreditsAdmin(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(paymentService.addCreditsAdmin(body, currentUser));
    }
}
