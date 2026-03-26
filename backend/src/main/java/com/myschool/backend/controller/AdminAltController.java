package com.myschool.backend.controller;

import com.myschool.backend.models.entity.User;
import com.myschool.backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminAltController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(@AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.getDashboardStats(currentUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user-logs")
    public ResponseEntity<Map<String, Object>> getUserLogs(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.getUserLogs(limit, skip, role, search, currentUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user-logs/stats")
    public ResponseEntity<Map<String, Object>> getUserLogStats(@AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.getUserLogStats(currentUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sales-plans")
    public ResponseEntity<Map<String, Object>> getSalesPlans(@AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.getSalesPlans(currentUser);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sales-plans")
    public ResponseEntity<Map<String, Object>> createSalesPlan(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.createSalesPlan(body, currentUser);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/sales-plans/{planId}")
    public ResponseEntity<Map<String, Object>> updateSalesPlan(
            @PathVariable String planId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.updateSalesPlan(planId, body, currentUser);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/sales-plans/{planId}/status")
    public ResponseEntity<Map<String, Object>> updateSalesPlanStatus(
            @PathVariable String planId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.updateSalesPlanStatus(planId, body, currentUser);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/sales-plans/{planId}")
    public ResponseEntity<Map<String, Object>> deleteSalesPlan(
            @PathVariable String planId,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.deleteSalesPlan(planId, currentUser);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bulk-upload/schools")
    public ResponseEntity<Map<String, Object>> bulkUploadSchools(
            @RequestBody List<Map<String, Object>> data,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.bulkUploadSchools(data, currentUser);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bulk-upload/teachers")
    public ResponseEntity<Map<String, Object>> bulkUploadTeachers(
            @RequestBody List<Map<String, Object>> data,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.bulkUploadUsers(data, currentUser);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bulk-upload/students")
    public ResponseEntity<Map<String, Object>> bulkUploadStudents(
            @RequestBody List<Map<String, Object>> data,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.bulkUploadUsers(data, currentUser);
        return ResponseEntity.ok(result);
    }
}
