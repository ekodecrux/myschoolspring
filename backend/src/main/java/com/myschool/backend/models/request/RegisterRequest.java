package com.myschool.backend.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 30, message = "Email must not exceed 30 characters")
    @JsonProperty("emailId")
    private String email;

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 40, message = "Name must be between 1 and 40 characters")
    private String name;

    private String password;

    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @JsonProperty("userRole")
    private String userRole = "INDIVIDUAL";

    @JsonProperty("schoolCode")
    @Size(max = 16, message = "School code must not exceed 16 characters")
    private String schoolCode;

    @JsonProperty("teacherCode")
    private String teacherCode;

    @JsonProperty("studentCode")
    private String studentCode;

    @Size(max = 100, message = "Address must not exceed 100 characters")
    private String address;

    private String city;
    private String state;

    @JsonProperty("postalCode")
    private String postalCode;

    @JsonProperty("className")
    private String className;

    @JsonProperty("sectionName")
    private String sectionName;

    @JsonProperty("rollNumber")
    private String rollNumber;

    @JsonProperty("fatherName")
    private String fatherName;

    @JsonProperty("principalName")
    private String principalName;
}
