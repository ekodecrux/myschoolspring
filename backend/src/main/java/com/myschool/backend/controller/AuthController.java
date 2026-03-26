package com.myschool.backend.controller;

import com.myschool.backend.models.entity.User;
import com.myschool.backend.models.request.*;
import com.myschool.backend.models.response.LoginResponse;
import com.myschool.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rest/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

        // Login with email and password, optionally with school code
    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest request) {
        Object result = authService.login(request);
        return ResponseEntity.ok(result);
    }

        // Self-registration for INDIVIDUAL users
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> result = authService.register(request);
        return ResponseEntity.ok(result);
    }

        // Handle new password challenge after first login
    @PostMapping("/newPasswordChallenge")
    public ResponseEntity<LoginResponse> newPasswordChallenge(@RequestBody Map<String, Object> body) {
        LoginResponse result = authService.newPasswordChallenge(body);
        return ResponseEntity.ok(result);
    }

        // Refresh access token using refresh token
    @PostMapping("/refreshToken")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = authService.refreshToken(body);
        return ResponseEntity.ok(result);
    }

        // Send password reset email
    @GetMapping("/forgotPassword")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestParam String email) {
        Map<String, Object> result = authService.forgotPassword(email);
        return ResponseEntity.ok(result);
    }

        // Reset password with OTP code
    @PostMapping("/confirmPassword")
    public ResponseEntity<Map<String, Object>> confirmPassword(
            @Valid @RequestBody ConfirmPasswordResetRequest request) {
        Map<String, Object> result = authService.confirmPassword(request);
        return ResponseEntity.ok(result);
    }

        // Change password for logged-in user
    @PostMapping("/changePassword")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = authService.changePassword(request, currentUser);
        return ResponseEntity.ok(result);
    }

        // Send OTP to phone number
    @GetMapping("/sendOtp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestParam String phoneNumber) {
        Map<String, Object> result = authService.sendOtp(phoneNumber);
        return ResponseEntity.ok(result);
    }

        // Login using OTP
    @PostMapping("/loginViaOtp")
    public ResponseEntity<LoginResponse> loginViaOtp(@RequestBody Map<String, Object> body) {
        LoginResponse result = authService.loginViaOtp(body);
        return ResponseEntity.ok(result);
    }
}
