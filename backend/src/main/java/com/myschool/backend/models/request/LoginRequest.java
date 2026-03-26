package com.myschool.backend.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LoginRequest {
    // Accept both "email" and "username" - frontend sends "email"
    private String email;
    private String username;
    private String password;

    @JsonProperty("schoolCode")
    private String schoolCode;

    // Convenience method - return whichever identifier was provided
    public String getIdentifier() {
        if (email != null && !email.isEmpty()) return email;
        if (username != null && !username.isEmpty()) return username;
        return null;
    }
}
