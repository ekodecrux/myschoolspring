package com.myschool.backend.service;

import com.myschool.backend.exception.AppException;
import com.myschool.backend.models.entity.LessonPlan;
import com.myschool.backend.models.entity.User;
import com.myschool.backend.repository.LessonPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LessonPlanService {

    @Autowired private LessonPlanRepository lessonPlanRepository;

    public Map<String, Object> listLessonPlans(User currentUser) {
        List<LessonPlan> plans = lessonPlanRepository.findByUserId(currentUser.getId());
        return Map.of(
                "data", plans.stream().map(this::buildPlanResponse).collect(Collectors.toList()),
                "total", plans.size()
        );
    }

    public Map<String, Object> getLessonPlan(String planId, User currentUser) {
        LessonPlan plan = lessonPlanRepository.findByIdAndUserId(planId, currentUser.getId())
                .orElseThrow(() -> new AppException("Lesson plan not found", HttpStatus.NOT_FOUND));
        return buildPlanResponse(plan);
    }

    public Map<String, Object> saveLessonPlan(Map<String, Object> body, User currentUser) {
        String id = (String) body.get("id");

        if (id != null && !id.isEmpty()) {
            Optional<LessonPlan> existing = lessonPlanRepository.findByIdAndUserId(id, currentUser.getId());
            if (existing.isPresent()) {
                LessonPlan plan = existing.get();
                updatePlanFromBody(plan, body);
                plan.setUpdatedAt(Instant.now().toString());
                lessonPlanRepository.save(plan);
                return Map.of("message", "Lesson plan updated", "id", plan.getId());
            }
        }

        String planId = UUID.randomUUID().toString();
        LessonPlan plan = LessonPlan.builder()
                .id(planId)
                .userId(currentUser.getId())
                .title((String) body.get("title"))
                .subject((String) body.get("subject"))
                .className((String) body.get("className"))
                .date((String) body.get("date"))
                .objectives((String) body.get("objectives"))
                .content((String) body.get("content"))
                .activities((String) body.get("activities"))
                .resources((String) body.get("resources"))
                .homework((String) body.get("homework"))
                .notes((String) body.get("notes"))
                .useDigitalBoard(body.get("useDigitalBoard") != null ? (Boolean) body.get("useDigitalBoard") : false)
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .build();

        lessonPlanRepository.save(plan);
        return Map.of("message", "Lesson plan created", "id", planId);
    }

    public Map<String, Object> deleteLessonPlan(String planId, User currentUser) {
        LessonPlan plan = lessonPlanRepository.findByIdAndUserId(planId, currentUser.getId())
                .orElseThrow(() -> new AppException("Lesson plan not found", HttpStatus.NOT_FOUND));
        lessonPlanRepository.delete(plan);
        return Map.of("message", "Lesson plan deleted");
    }

    private void updatePlanFromBody(LessonPlan plan, Map<String, Object> body) {
        if (body.get("title") != null) plan.setTitle((String) body.get("title"));
        if (body.get("subject") != null) plan.setSubject((String) body.get("subject"));
        if (body.get("className") != null) plan.setClassName((String) body.get("className"));
        if (body.get("date") != null) plan.setDate((String) body.get("date"));
        if (body.get("objectives") != null) plan.setObjectives((String) body.get("objectives"));
        if (body.get("content") != null) plan.setContent((String) body.get("content"));
        if (body.get("activities") != null) plan.setActivities((String) body.get("activities"));
        if (body.get("resources") != null) plan.setResources((String) body.get("resources"));
        if (body.get("homework") != null) plan.setHomework((String) body.get("homework"));
        if (body.get("notes") != null) plan.setNotes((String) body.get("notes"));
        if (body.get("useDigitalBoard") != null) plan.setUseDigitalBoard((Boolean) body.get("useDigitalBoard"));
    }

    private Map<String, Object> buildPlanResponse(LessonPlan p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("title", p.getTitle());
        map.put("subject", p.getSubject());
        map.put("className", p.getClassName());
        map.put("date", p.getDate());
        map.put("objectives", p.getObjectives());
        map.put("content", p.getContent());
        map.put("activities", p.getActivities());
        map.put("resources", p.getResources());
        map.put("homework", p.getHomework());
        map.put("notes", p.getNotes());
        map.put("useDigitalBoard", p.getUseDigitalBoard());
        map.put("createdAt", p.getCreatedAt());
        map.put("updatedAt", p.getUpdatedAt());
        return map;
    }
}
