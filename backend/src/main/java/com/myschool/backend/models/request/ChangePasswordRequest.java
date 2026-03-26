package com.myschool.backend.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @JsonProperty("currentPassword")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @JsonProperty("newPassword")
    private String newPassword;
}
