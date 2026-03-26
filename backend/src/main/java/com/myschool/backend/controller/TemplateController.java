package com.myschool.backend.controller;

import com.myschool.backend.models.entity.User;
import com.myschool.backend.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rest/templates")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    /**
     * GET /api/rest/templates/list
     * List user templates and system templates
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listTemplates(
            @RequestParam(required = false) String makerType,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = templateService.listTemplates(makerType, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/rest/templates/save
     * Save or update a template
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveTemplate(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = templateService.saveTemplate(body, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/rest/templates/{templateId}
     * Delete a template
     */
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Map<String, Object>> deleteTemplate(
            @PathVariable String templateId,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = templateService.deleteTemplate(templateId, currentUser);
        return ResponseEntity.ok(result);
    }
}
