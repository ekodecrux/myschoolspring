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

    /**
     * GET /api/rest/users/details
     * Get current logged-in user details
     */
    @GetMapping({"/details", "/getUserDetails"})
    public ResponseEntity<Map<String, Object>> getUserDetails(@AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.getUserDetails(currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/rest/users/update
     * Update user details (name, mobile, address etc.)
     */
    @RequestMapping(value = {"/update", "/updateUserDetails"}, method = {org.springframework.web.bind.annotation.RequestMethod.PUT, org.springframework.web.bind.annotation.RequestMethod.PATCH})
    public ResponseEntity<Map<String, Object>> updateUserDetails(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.updateUserDetails(body, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/rest/users/list
     * List users (filtered by role, school, teacher)
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "100") int limit,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.listUsers(role, limit, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/rest/users/search
     * Search users by query string
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestParam String query,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "100") int limit,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.searchUsers(query, role, limit, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/rest/users/add
     * Add a new user (teacher/student/parent) by admin or teacher
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addUser(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.addUser(request, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/rest/users/updateCredits
     * Update user credits (add/remove/set)
     */
    @PostMapping("/updateCredits")
    public ResponseEntity<Map<String, Object>> updateCredits(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.updateCredits(body, currentUser);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{userId}/disable")
    public ResponseEntity<Map<String, Object>> disableAccount(
            @PathVariable String userId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.disableAccount(userId, currentUser));
    }

    // Admin update any user's details (matches FastAPI /adminUpdateUser)
    @PatchMapping("/adminUpdateUser")
    public ResponseEntity<Map<String, Object>> adminUpdateUser(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.adminUpdateUser(body, currentUser));
    }

    // Check current user's credits and subscription status
    @GetMapping("/checkCredits")
    public ResponseEntity<Map<String, Object>> checkCredits(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.checkCredits(currentUser));
    }

    // Deduct credits when user downloads/uses a resource
    @PostMapping("/useCredits")
    public ResponseEntity<Map<String, Object>> useCredits(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.useCredits(body, currentUser));
    }

    // School Admin requests credits from Super Admin
    @PostMapping("/purchaseCredits")
    public ResponseEntity<Map<String, Object>> purchaseCredits(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.purchaseCredits(body, currentUser));
    }

    // Get user's subscription/payment history
    @GetMapping("/subscription-history")
    public ResponseEntity<Map<String, Object>> getSubscriptionHistory(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getSubscriptionHistory(currentUser));
    }

    // GET /api/rest/users/listUsersByRole?role=TEACHER&limit=100&schoolCode=...
    // Called by Teacher.jsx, Student.jsx, School.jsx via Redux ListUsers thunk
    @GetMapping("/listUsersByRole")
    public ResponseEntity<Map<String, Object>> listUsersByRole(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String schoolCode,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = userService.listUsersByRole(role, limit, schoolCode, search, currentUser);
        return ResponseEntity.ok(result);
    }

    // POST /api/rest/users/disableAccount
    @PostMapping("/disableAccount")
    public ResponseEntity<Map<String, Object>> disableAccount(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        String userId = (String) body.get("userId");
        Map<String, Object> result = userService.disableAccount(userId, currentUser);
        return ResponseEntity.ok(result);
    }



    // PATCH /api/rest/users/updateCredits  (alias – body-based version)

    // POST /api/rest/users/add  (alias for the existing addUser)
    // Note: /add already exists via @PostMapping("/add"), this is a no-op guard

}
