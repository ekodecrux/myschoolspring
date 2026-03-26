package com.myschool.backend.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "orders")
public class Order {

    @Id
    private String mongoId;

    @Indexed(unique = true)
    private String id;

    @Indexed
    @Field("user_id")
    private String userId;

    private String status;

    @Field("plan_type")
    private String planType;

    @Field("plan_name")
    private String planName;

    private Double amount;
    private Integer credits;
    private String currency;

    // Razorpay fields
    @Field("razorpay_order_id")
    private String razorpayOrderId;

    @Field("razorpay_payment_id")
    private String razorpayPaymentId;

    @Field("razorpay_signature")
    private String razorpaySignature;

    // Stripe fields
    @Field("stripe_session_id")
    private String stripeSessionId;

    @Field("stripe_payment_intent")
    private String stripePaymentIntent;

    @Field("created_at")
    private String createdAt;

    @Field("completed_at")
    private String completedAt;
}
