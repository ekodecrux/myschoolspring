package com.myschool.backend.controller;

import com.myschool.backend.models.entity.User;
import com.myschool.backend.service.LessonPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rest/lesson-plans")
public class LessonPlanController {

    @Autowired
    private LessonPlanService lessonPlanService;

    /**
     * GET /api/rest/lesson-plans/list
     * List all lesson plans for current user
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listLessonPlans(@AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = lessonPlanService.listLessonPlans(currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/rest/lesson-plans/{planId}
     * Get a specific lesson plan
     */
    @GetMapping("/{planId}")
    public ResponseEntity<Map<String, Object>> getLessonPlan(
            @PathVariable String planId,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = lessonPlanService.getLessonPlan(planId, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/rest/lesson-plans/save
     * Save or update a lesson plan
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveLessonPlan(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = lessonPlanService.saveLessonPlan(body, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/rest/lesson-plans/{planId}
     * Delete a lesson plan
     */
    @DeleteMapping("/{planId}")
    public ResponseEntity<Map<String, Object>> deleteLessonPlan(
            @PathVariable String planId,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = lessonPlanService.deleteLessonPlan(planId, currentUser);
        return ResponseEntity.ok(result);
    }
}
