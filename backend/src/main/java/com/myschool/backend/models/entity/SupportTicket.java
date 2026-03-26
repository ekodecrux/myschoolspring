package com.myschool.backend.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "support_tickets")
public class SupportTicket {

    @Id
    private String mongoId;

    private String id;

    @Field("user_id")
    private String userId;

    @Field("user_email")
    private String userEmail;

    @Field("user_name")
    private String userName;

    private String subject;
    private String message;
    private String category;
    private String priority;
    private String status;

    private List<Map<String, Object>> replies;

    @Field("created_at")
    private String createdAt;

    @Field("updated_at")
    private String updatedAt;

    @Field("resolved_at")
    private String resolvedAt;
}
