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
@Document(collection = "schools")
public class School {

    @Id
    private String mongoId;

    @Indexed(unique = true)
    private String id;

    private String name;

    @Indexed(unique = true)
    private String code;

    @Field("principal_name")
    private String principalName;

    private String address;
    private String city;
    private String state;

    @Field("postal_code")
    private String postalCode;

    private String phone;
    private String email;

    @Field("logo_url")
    private String logoUrl;

    @Field("admin_id")
    private String adminId;

    @Field("admin_email")
    private String adminEmail;

    @Field("is_active")
    private Boolean isActive;

    private Integer credits;

    @Field("created_at")
    private String createdAt;

    @Field("created_by")
    private String createdBy;

    @Field("updated_at")
    private String updatedAt;
}
