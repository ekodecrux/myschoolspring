package com.myschool.backend.service;

import com.myschool.backend.exception.AppException;
import com.myschool.backend.models.entity.Order;
import com.myschool.backend.models.entity.User;
import com.myschool.backend.models.entity.UserRole;
import com.myschool.backend.repository.OrderRepository;
import com.myschool.backend.repository.UserRepository;
import com.razorpay.RazorpayClient;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MongoTemplate mongoTemplate;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${stripe.api-key:}")
    private String stripeApiKey;

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    private RazorpayClient razorpayClient;

    @PostConstruct
    public void init() {
        if (razorpayKeyId != null && !razorpayKeyId.isEmpty()) {
            try {
                razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
                logger.info("Razorpay client initialized");
            } catch (Exception e) {
                logger.warn("Failed to initialize Razorpay: {}", e.getMessage());
            }
        }
        if (stripeApiKey != null && !stripeApiKey.isEmpty()) {
            Stripe.apiKey = stripeApiKey;
            logger.info("Stripe initialized");
        }
    }

    // ======================== SUBSCRIPTION PLANS ========================

    private static final Map<String, Map<String, Object>> SUBSCRIPTION_PLANS = Map.of(
            "basic", Map.of("name", "Basic Plan", "credits", 500, "amount", 99.0, "currency", "INR"),
            "standard", Map.of("name", "Standard Plan", "credits", 1500, "amount", 249.0, "currency", "INR"),
            "premium", Map.of("name", "Premium Plan", "credits", 5000, "amount", 699.0, "currency", "INR"),
            "enterprise", Map.of("name", "Enterprise Plan", "credits", 15000, "amount", 1499.0, "currency", "INR")
    );

    private static final Map<String, Map<String, Object>> CREDIT_PACKS = Map.of(
            "pack_100", Map.of("name", "100 Credits Pack", "credits", 100, "amount", 29.0, "currency", "INR"),
            "pack_500", Map.of("name", "500 Credits Pack", "credits", 500, "amount", 99.0, "currency", "INR"),
            "pack_1000", Map.of("name", "1000 Credits Pack", "credits", 1000, "amount", 179.0, "currency", "INR"),
            "pack_5000", Map.of("name", "5000 Credits Pack", "credits", 5000, "amount", 699.0, "currency", "INR")
    );

    public Map<String, Object> getPlans() {
        // Build flat array of plans for frontend payment.jsx (expects response.data.plans)
        java.util.List<Map<String, Object>> plansList = new java.util.ArrayList<>();
        String[] planOrder = {"basic", "standard", "premium", "enterprise"};
        for (String key : planOrder) {
            Map<String, Object> p = SUBSCRIPTION_PLANS.get(key);
            if (p != null) {
                java.util.Map<String, Object> plan = new java.util.LinkedHashMap<>(p);
                plan.put("id", key);
                plan.put("type", "subscription");
                plansList.add(plan);
            }
        }
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("plans", plansList);
        result.put("subscriptions", SUBSCRIPTION_PLANS);
        result.put("creditPacks", CREDIT_PACKS);
        return result;
    }

    // ======================== RAZORPAY ========================

    public Map<String, Object> createRazorpayOrder(Map<String, Object> body, User currentUser) {
        if (razorpayClient == null) {
            throw new AppException("Razorpay is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }

        String planType = body.get("plan_type") != null ? (String) body.get("plan_type") : (String) body.get("planType");
        String planId = body.get("plan_id") != null ? (String) body.get("plan_id") : (String) body.get("planId");

        Map<String, Object> plan = getPlan(planType, planId);
        if (plan == null) {
            throw new AppException("Invalid plan", HttpStatus.BAD_REQUEST);
        }

        double amount = ((Number) plan.get("amount")).doubleValue();
        int credits = ((Number) plan.get("credits")).intValue();
        String planName = (String) plan.get("name");
        String currency = (String) plan.getOrDefault("currency", "INR");

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int) (amount * 100)); // paise
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "order_" + UUID.randomUUID().toString().substring(0, 8));

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            String orderId = UUID.randomUUID().toString();
            Order order = Order.builder()
                    .id(orderId)
                    .userId(currentUser.getId())
                    .status("pending")
                    .planType(planType)
                    .planName(planName)
                    .amount(amount)
                    .credits(credits)
                    .currency(currency)
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .createdAt(Instant.now().toString())
                    .build();
            orderRepository.save(order);

            return Map.of(
                    "orderId", orderId,
                    "razorpayOrderId", razorpayOrder.get("id").toString(),
                    "amount", amount,
                    "currency", currency,
                    "planName", planName,
                    "credits", credits,
                    "keyId", razorpayKeyId
            );
        } catch (Exception e) {
            throw new AppException("Failed to create Razorpay order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, Object> verifyRazorpayPayment(Map<String, Object> body, User currentUser) {
        String razorpayOrderId = (String) body.get("razorpayOrderId");
        String razorpayPaymentId = (String) body.get("razorpayPaymentId");
        String razorpaySignature = (String) body.get("razorpaySignature");

        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
            throw new AppException("Missing payment verification fields", HttpStatus.BAD_REQUEST);
        }

        // Verify signature
        String message = razorpayOrderId + "|" + razorpayPaymentId;
        if (!verifyHmacSha256(message, razorpayKeySecret, razorpaySignature)) {
            throw new AppException("Payment verification failed - invalid signature", HttpStatus.BAD_REQUEST);
        }

        // Find and update order
        Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new AppException("Order not found", HttpStatus.NOT_FOUND));

        order.setStatus("completed");
        order.setRazorpayPaymentId(razorpayPaymentId);
        order.setRazorpaySignature(razorpaySignature);
        order.setCompletedAt(Instant.now().toString());
        orderRepository.save(order);

        // Add credits to user
        User user = userRepository.findByIdField(currentUser.getId())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        int currentCredits = user.getCredits() != null ? user.getCredits() : 0;
        user.setCredits(currentCredits + order.getCredits());
        user.setSubscriptionStatus("active");
        user.setLastSubscriptionPlan(order.getPlanName());
        user.setLastSubscriptionDate(Instant.now().toString());
        userRepository.save(user);

        return Map.of(
                "message", "Payment verified and credits added successfully",
                "creditsAdded", order.getCredits(),
                "newBalance", user.getCredits()
        );
    }

    // ======================== STRIPE ========================

    public Map<String, Object> createStripeSession(Map<String, Object> body, User currentUser) {
        if (stripeApiKey == null || stripeApiKey.isEmpty()) {
            throw new AppException("Stripe is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }

        String planType = body.get("plan_type") != null ? (String) body.get("plan_type") : (String) body.get("planType");
        String planId = body.get("plan_id") != null ? (String) body.get("plan_id") : (String) body.get("planId");
        String successUrl = (String) body.getOrDefault("successUrl", "https://portal.myschoolct.com/payment/success");
        String cancelUrl = (String) body.getOrDefault("cancelUrl", "https://portal.myschoolct.com/payment/cancel");

        Map<String, Object> plan = getPlan(planType, planId);
        if (plan == null) {
            throw new AppException("Invalid plan", HttpStatus.BAD_REQUEST);
        }

        double amount = ((Number) plan.get("amount")).doubleValue();
        int credits = ((Number) plan.get("credits")).intValue();
        String planName = (String) plan.get("name");

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .setCustomerEmail(currentUser.getEmail())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount((long) (amount * 100))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(planName)
                                                                    .setDescription(credits + " Credits")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("userId", currentUser.getId())
                    .putMetadata("planType", planType)
                    .putMetadata("planId", planId != null ? planId : "")
                    .putMetadata("credits", String.valueOf(credits))
                    .build();

            Session session = Session.create(params);

            String orderId = UUID.randomUUID().toString();
            Order order = Order.builder()
                    .id(orderId)
                    .userId(currentUser.getId())
                    .status("pending")
                    .planType(planType)
                    .planName(planName)
                    .amount(amount)
                    .credits(credits)
                    .currency("USD")
                    .stripeSessionId(session.getId())
                    .createdAt(Instant.now().toString())
                    .build();
            orderRepository.save(order);

            return Map.of(
                    "sessionId", session.getId(),
                    "sessionUrl", session.getUrl(),
                    "orderId", orderId
            );
        } catch (Exception e) {
            throw new AppException("Failed to create Stripe session: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, Object> stripeWebhook(String payload, String sigHeader) {
        // Verify webhook signature and process
        try {
            com.stripe.model.Event event = com.stripe.net.Webhook.constructEvent(
                    payload, sigHeader, stripeWebhookSecret);

            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session != null) {
                    processStripePayment(session);
                }
            }
            return Map.of("status", "success");
        } catch (Exception e) {
            throw new AppException("Webhook error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private void processStripePayment(Session session) {
        Order order = orderRepository.findByStripeSessionId(session.getId()).orElse(null);
        if (order == null) return;

        order.setStatus("completed");
        order.setStripePaymentIntent(session.getPaymentIntent());
        order.setCompletedAt(Instant.now().toString());
        orderRepository.save(order);

        User user = userRepository.findByIdField(order.getUserId()).orElse(null);
        if (user != null) {
            int currentCredits = user.getCredits() != null ? user.getCredits() : 0;
            user.setCredits(currentCredits + order.getCredits());
            user.setSubscriptionStatus("active");
            user.setLastSubscriptionPlan(order.getPlanName());
            user.setLastSubscriptionDate(Instant.now().toString());
            userRepository.save(user);
        }
    }

    public Map<String, Object> getOrderHistory(User currentUser) {
        List<Order> orders = orderRepository.findByUserId(currentUser.getId());
        return Map.of("data", orders, "total", orders.size());
    }

    // ======================== ADMIN ========================

    public Map<String, Object> addCreditsAdmin(Map<String, Object> body, User currentUser) {
        if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        String userId = (String) body.get("userId");
        int credits = ((Number) body.get("credits")).intValue();

        User targetUser = userRepository.findByIdField(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        int currentCredits = targetUser.getCredits() != null ? targetUser.getCredits() : 0;
        targetUser.setCredits(currentCredits + credits);
        userRepository.save(targetUser);

        return Map.of(
                "message", "Credits added successfully",
                "newBalance", targetUser.getCredits()
        );
    }

    private Map<String, Object> getPlan(String planType, String planId) {
        if (planType == null) return null;
        // Direct plan name lookup (e.g. plan_type="basic" -> subscription plan)
        if (SUBSCRIPTION_PLANS.containsKey(planType)) {
            return SUBSCRIPTION_PLANS.get(planType);
        }
        if (CREDIT_PACKS.containsKey(planType)) {
            return CREDIT_PACKS.get(planType);
        }
        // Legacy: planType="subscription" + planId="basic"
        if ("subscription".equals(planType) && planId != null) {
            return SUBSCRIPTION_PLANS.get(planId);
        } else if ("credits".equals(planType) && planId != null) {
            return CREDIT_PACKS.get(planId);
        }
        return null;
    }

    private boolean verifyHmacSha256(String message, String secret, String expectedSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString().equals(expectedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> getUserCredits(User currentUser) {
        String role = currentUser.getRole() != null ? currentUser.getRole().toString() : "";
        if ("SUPER_ADMIN".equals(role)) {
            return Map.of(
                "credits", -1,
                "isUnlimited", true,
                "display", "Unlimited"
            );
        }
        int credits = currentUser.getCredits() != null ? currentUser.getCredits() : 0;
        return Map.of(
            "credits", credits,
            "isUnlimited", false,
            "display", String.valueOf(credits)
        );
    }
}
