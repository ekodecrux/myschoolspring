package com.myschool.backend.controller;

import com.myschool.backend.models.entity.User;
import com.myschool.backend.models.request.CreateSchoolRequest;
import com.myschool.backend.service.SchoolService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rest/schools")
public class SchoolController {

    @Autowired
    private SchoolService schoolService;

    /**
     * POST /api/rest/schools/create
     * Create a new school with School Admin (Super Admin only)
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> createSchool(
            @Valid @RequestBody CreateSchoolRequest request,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = schoolService.createSchool(request, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/rest/schools/list
     * List all schools (Super Admin only)
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> listSchools(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int skip) {
        Map<String, Object> result = schoolService.listSchools(limit, skip);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/rest/schools/public/active
     * Get list of active schools for public registration dropdown (no auth required)
     */
    @GetMapping("/public/active")
    public ResponseEntity<Map<String, Object>> getActiveSchoolsPublic() {
        List<Map<String, Object>> schools = schoolService.getActiveSchoolsPublic();
        return ResponseEntity.ok(Map.of("schools", schools));
    }

    /**
     * GET /api/rest/schools/{schoolCode}
     * Get school details
     */
    @GetMapping("/{schoolCode}")
    public ResponseEntity<Map<String, Object>> getSchool(
            @PathVariable String schoolCode,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = schoolService.getSchool(schoolCode, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /api/rest/schools/{schoolCode}/toggle-status
     * Enable/Disable school (Super Admin only)
     */
    @PatchMapping("/{schoolCode}/toggle-status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> toggleSchoolStatus(@PathVariable String schoolCode) {
        Map<String, Object> result = schoolService.toggleSchoolStatus(schoolCode);
        return ResponseEntity.ok(result);
    }
}
