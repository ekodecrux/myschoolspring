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
@Document(collection = "password_resets")
public class PasswordReset {

    @Id
    private String mongoId;

    private String email;
    private String code;

    @Field("created_at")
    private String createdAt;

    @Field("expires_at")
    private String expiresAt;
}
