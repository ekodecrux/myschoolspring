package com.myschool.backend.models.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String mongoId;

    @Indexed(unique = true)
    private String id;

    @Indexed(unique = true)
    private String email;

    @JsonIgnore
    @Field("password_hash")
    private String passwordHash;

    private String name;

    private String role;

    @Field("school_code")
    @Indexed
    private String schoolCode;

    @Field("teacher_code")
    @Indexed
    private String teacherCode;

    @Field("student_code")
    private String studentCode;

    @Field("mobile_number")
    @Indexed
    private String mobileNumber;

    private String address;
    private String city;
    private String state;

    @Field("postal_code")
    private String postalCode;

    @Field("father_name")
    private String fatherName;

    @Field("principal_name")
    private String principalName;

    @Field("class_name")
    private String className;

    @Field("section_name")
    private String sectionName;

    @Field("roll_number")
    private String rollNumber;

    @Field("school_name")
    private String schoolName;

    private Integer credits;

    private Boolean disabled;

    @Field("require_password_change")
    private Boolean requirePasswordChange;

    @Field("subscription_status")
    private String subscriptionStatus;

    @Field("last_subscription_plan")
    private String lastSubscriptionPlan;

    @Field("last_subscription_date")
    private String lastSubscriptionDate;

    @Field("created_at")
    private String createdAt;

    @Field("updated_at")
    private String updatedAt;

    @Field("last_login")
    private Instant lastLogin;

    @Field("added_by")
    private String addedBy;

    @Field("created_by")
    private String createdBy;

    @Field("registration_type")
    private String registrationType;
}
