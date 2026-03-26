package com.myschool.backend.controller;

import com.myschool.backend.models.entity.User;
import com.myschool.backend.models.request.RegisterRequest;
import com.myschool.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rest/users")
public class UserController {

    @Autowired
    private UserService userService;

        // Get current logged-in user details
    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getUserDetails(@AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.getUserDetails(currentUser);
        return ResponseEntity.ok(result);
    }

        // Update user details (name, mobile, address etc.)
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateUserDetails(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.updateUserDetails(body, currentUser);
        return ResponseEntity.ok(result);
    }

        // List users (filtered by role, school, teacher)
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "100") int limit,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.listUsers(role, limit, currentUser);
        return ResponseEntity.ok(result);
    }

        // Search users by query string
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestParam String query,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "100") int limit,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.searchUsers(query, role, limit, currentUser);
        return ResponseEntity.ok(result);
    }

        // Add a new user (teacher/student/parent) by admin or teacher
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addUser(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.addUser(request, currentUser);
        return ResponseEntity.ok(result);
    }

        // Update user credits (add/remove/set)
    @PostMapping("/updateCredits")
    public ResponseEntity<Map<String, Object>> updateCredits(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.updateCredits(body, currentUser);
        return ResponseEntity.ok(result);
    }

        // Enable/Disable a user account
    @PatchMapping("/{userId}/disable")
    public ResponseEntity<Map<String, Object>> disableAccount(
            @PathVariable String userId,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.disableAccount(userId, currentUser);
        return ResponseEntity.ok(result);
    }
}
