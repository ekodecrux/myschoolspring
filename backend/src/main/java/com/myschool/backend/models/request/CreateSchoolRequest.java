package com.myschool.backend.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSchoolRequest {

    @NotBlank(message = "School name is required")
    private String name;

    @JsonProperty("principalName")
    private String principalName;

    private String address;
    private String city;
    private String state;

    @JsonProperty("postalCode")
    private String postalCode;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Invalid admin email")
    @JsonProperty("adminEmail")
    private String adminEmail;

    @NotBlank(message = "Admin name is required")
    @JsonProperty("adminName")
    private String adminName;

    @JsonProperty("adminPhone")
    private String adminPhone;
}
