package com.myschool.backend.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "payments")
public class Payment {

    @Id
    private String mongoId;

    private String id;

    @Field("user_id")
    private String userId;

    @Field("razorpay_order_id")
    private String razorpayOrderId;

    @Field("razorpay_payment_id")
    private String razorpayPaymentId;

    @Field("plan_type")
    private String planType;

    @Field("plan_name")
    private String planName;

    private Double amount;

    @Field("credits_added")
    private Integer creditsAdded;

    private String status;

    @Field("created_at")
    private String createdAt;
}
