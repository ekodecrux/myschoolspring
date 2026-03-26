package com.myschool.backend.controller;

import com.myschool.backend.models.entity.User;
import com.myschool.backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rest/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

        // Get platform-wide stats (Super Admin only)
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats(@AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.getDashboardStats(currentUser);
        return ResponseEntity.ok(result);
    }

        // Bulk upload users from CSV/Excel data
    @PostMapping("/bulk-upload-users")
    public ResponseEntity<Map<String, Object>> bulkUploadUsers(
            @RequestBody List<Map<String, Object>> users,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.bulkUploadUsers(users, currentUser);
        return ResponseEntity.ok(result);
    }

        // Update a system template (Super Admin only)
    @PutMapping("/templates/{templateId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateSystemTemplate(
            @PathVariable String templateId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = adminService.updateSystemTemplate(templateId, body, currentUser);
        return ResponseEntity.ok(result);
    }
}
