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
@Document(collection = "otp_sessions")
public class OtpSession {

    @Id
    private String mongoId;

    @Field("session_id")
    private String sessionId;

    @Field("phone_number")
    private String phoneNumber;

    private String otp;

    @Field("created_at")
    private String createdAt;

    @Field("expires_at")
    private String expiresAt;
}
