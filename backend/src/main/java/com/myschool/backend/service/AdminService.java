package com.myschool.backend.service;

import com.myschool.backend.exception.AppException;
import com.myschool.backend.models.entity.User;
import com.myschool.backend.models.entity.UserRole;
import com.myschool.backend.repository.OrderRepository;
import com.myschool.backend.repository.ResourceImageRepository;
import com.myschool.backend.repository.SchoolRepository;
import com.myschool.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class AdminService {

    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ResourceImageRepository resourceImageRepository;
    @Autowired private MongoTemplate mongoTemplate;

    public Map<String, Object> getDashboardStats(User currentUser) {
        if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        long totalUsers = userRepository.count();
        long totalSchools = schoolRepository.count();
        long totalOrders = orderRepository.count();
        long totalImages = resourceImageRepository.count();
        long pendingImages = mongoTemplate.count(
                new Query(Criteria.where("status").is("pending")),
                com.myschool.backend.models.entity.ResourceImage.class);

        // Count by role
        long superAdmins = userRepository.countByRole(UserRole.SUPER_ADMIN);
        long schoolAdmins = userRepository.countByRole(UserRole.SCHOOL_ADMIN);
        long teachers = userRepository.countByRole(UserRole.TEACHER);
        long students = userRepository.countByRole(UserRole.STUDENT);
        long parents = userRepository.countByRole(UserRole.PARENT);
        long individuals = userRepository.countByRole(UserRole.INDIVIDUAL);

        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalSchools", schoolAdmins);
        stats.put("totalTeachers", teachers);
        stats.put("totalStudents", students);
        stats.put("totalImages", totalImages);
        stats.put("pendingImages", pendingImages);
        stats.put("totalOrders", totalOrders);
        stats.put("activeUsers", totalUsers);
        stats.put("disabledUsers", 0L);
        stats.put("totalCreditsUsed", 0L);
        stats.put("recentActivity", java.util.List.of());
        stats.put("userRole", currentUser.getRole());
        java.util.Map<String, Object> byRole = new java.util.LinkedHashMap<>();
        byRole.put("superAdmins", superAdmins);
        byRole.put("schoolAdmins", schoolAdmins);
        byRole.put("teachers", teachers);
        byRole.put("students", students);
        byRole.put("parents", parents);
        byRole.put("individuals", individuals);
        stats.put("usersByRole", byRole);
        return stats;
    }

    public Map<String, Object> bulkUploadUsers(List<Map<String, Object>> users, User currentUser) {
        if (!("SUPER_ADMIN".equals(currentUser.getRole()) || "SCHOOL_ADMIN".equals(currentUser.getRole()))) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        int success = 0;
        int failed = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        for (Map<String, Object> userData : users) {
            try {
                String email = (String) userData.get("email");
                if (email == null || email.isEmpty()) {
                    errors.add(Map.of("data", userData, "error", "Email is required"));
                    failed++;
                    continue;
                }

                if (userRepository.findByEmail(email).isPresent()) {
                    errors.add(Map.of("data", userData, "error", "Email already exists: " + email));
                    failed++;
                    continue;
                }

                success++;
            } catch (Exception e) {
                errors.add(Map.of("data", userData, "error", e.getMessage()));
                failed++;
            }
        }

        return Map.of(
                "message", "Bulk upload completed",
                "success", success,
                "failed", failed,
                "errors", errors
        );
    }

    public Map<String, Object> updateSystemTemplate(String templateId, Map<String, Object> body, User currentUser) {
        if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        Query query = new Query(Criteria.where("id").is(templateId));
        Update update = new Update();
        body.forEach((k, v) -> {
            if (!k.equals("id") && v != null) update.set(k, v);
        });
        update.set("updatedAt", Instant.now().toString());
        mongoTemplate.updateFirst(query, update, com.myschool.backend.models.entity.MakerTemplate.class);

        return Map.of("message", "System template updated");
    }

    public Map<String, Object> getUserLogs(int limit, int skip, String role, String search, User currentUser) {
        try {
            org.bson.Document query = new org.bson.Document();
            if (role != null && !role.isEmpty()) query.append("role", role);
            if (search != null && !search.isEmpty()) {
                org.bson.Document searchDoc = new org.bson.Document("$regex", search).append("$options", "i");
                java.util.List<org.bson.Document> orList = java.util.Arrays.asList(
                    new org.bson.Document("name", searchDoc),
                    new org.bson.Document("email", searchDoc)
                );
                query.append("$or", orList);
            }
            com.mongodb.client.MongoCollection<org.bson.Document> col = mongoTemplate.getDb().getCollection("users");
            java.util.List<Map<String, Object>> users = new java.util.ArrayList<>();
            col.find(query).skip(skip).limit(limit).sort(new org.bson.Document("created_at", -1))
                .forEach(doc -> {
                    Map<String, Object> u = new java.util.LinkedHashMap<>();
                    u.put("id", doc.get("id"));
                    u.put("name", doc.get("name"));
                    u.put("email", doc.get("email"));
                    u.put("role", doc.get("role"));
                    u.put("created_at", doc.get("created_at"));
                    u.put("disabled", doc.get("disabled"));
                    users.add(u);
                });
            long total = col.countDocuments(query);
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("users", users);
            result.put("total", total);
            result.put("hasMore", skip + limit < total);
            return result;
        } catch (Exception e) {
            return java.util.Map.of("users", java.util.List.of(), "total", 0, "hasMore", false);
        }
    }

    public Map<String, Object> getUserLogStats(User currentUser) {
        try {
            com.mongodb.client.MongoCollection<org.bson.Document> col = mongoTemplate.getDb().getCollection("users");
            long total = col.countDocuments(new org.bson.Document());
            long active = col.countDocuments(new org.bson.Document("disabled", new org.bson.Document("$ne", true)));
            long disabled = col.countDocuments(new org.bson.Document("disabled", true));
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("totalUsers", total);
            result.put("activeUsers", active);
            result.put("disabledUsers", disabled);
            return result;
        } catch (Exception e) {
            return java.util.Map.of("totalUsers", 0, "activeUsers", 0, "disabledUsers", 0);
        }
    }

    public Map<String, Object> getSalesPlans(User currentUser) {
        try {
            com.mongodb.client.MongoCollection<org.bson.Document> col = mongoTemplate.getDb().getCollection("sales_plans");
            java.util.List<Map<String, Object>> plans = new java.util.ArrayList<>();
            col.find().forEach(doc -> {
                Map<String, Object> p = new java.util.LinkedHashMap<>();
                doc.forEach((k, v) -> p.put(k, v instanceof org.bson.types.ObjectId ? v.toString() : v));
                if (!p.containsKey("id") && p.containsKey("_id")) p.put("id", p.get("_id").toString());
                plans.add(p);
            });
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("plans", plans);
            result.put("total", plans.size());
            return result;
        } catch (Exception e) {
            return java.util.Map.of("plans", java.util.List.of(), "total", 0);
        }
    }

    public Map<String, Object> createSalesPlan(Map<String, Object> body, User currentUser) {
        try {
            com.mongodb.client.MongoCollection<org.bson.Document> col = mongoTemplate.getDb().getCollection("sales_plans");
            org.bson.Document doc = new org.bson.Document(body);
            String planId = java.util.UUID.randomUUID().toString();
            doc.put("id", planId);
            doc.put("created_at", java.time.Instant.now().toString());
            col.insertOne(doc);
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("message", "Plan created");
            result.put("id", planId);
            return result;
        } catch (Exception e) {
            throw new com.myschool.backend.exception.AppException("Failed to create plan: " + e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, Object> updateSalesPlan(String planId, Map<String, Object> body, User currentUser) {
        try {
            com.mongodb.client.MongoCollection<org.bson.Document> col = mongoTemplate.getDb().getCollection("sales_plans");
            org.bson.Document update = new org.bson.Document("$set", new org.bson.Document(body));
            col.updateOne(new org.bson.Document("id", planId), update);
            return java.util.Map.of("message", "Plan updated");
        } catch (Exception e) {
            throw new com.myschool.backend.exception.AppException("Failed to update plan: " + e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, Object> updateSalesPlanStatus(String planId, Map<String, Object> body, User currentUser) {
        try {
            com.mongodb.client.MongoCollection<org.bson.Document> col = mongoTemplate.getDb().getCollection("sales_plans");
            Object status = body.get("status");
            org.bson.Document update = new org.bson.Document("$set", new org.bson.Document("status", status));
            col.updateOne(new org.bson.Document("id", planId), update);
            return java.util.Map.of("message", "Status updated");
        } catch (Exception e) {
            throw new com.myschool.backend.exception.AppException("Failed to update status: " + e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, Object> deleteSalesPlan(String planId, User currentUser) {
        try {
            com.mongodb.client.MongoCollection<org.bson.Document> col = mongoTemplate.getDb().getCollection("sales_plans");
            col.deleteOne(new org.bson.Document("id", planId));
            return java.util.Map.of("message", "Plan deleted");
        } catch (Exception e) {
            throw new com.myschool.backend.exception.AppException("Failed to delete plan: " + e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, Object> bulkUploadSchools(java.util.List<Map<String, Object>> data, User currentUser) {
        int created = 0;
        int failed = 0;
        for (Map<String, Object> row : data) {
            try {
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongoTemplate.getDb().getCollection("users");
                org.bson.Document doc = new org.bson.Document(row);
                if (!doc.containsKey("id")) doc.put("id", java.util.UUID.randomUUID().toString());
                if (!doc.containsKey("role")) doc.put("role", "SCHOOL_ADMIN");
                doc.put("created_at", java.time.Instant.now().toString());
                col.insertOne(doc);
                created++;
            } catch (Exception e) {
                failed++;
            }
        }
        return java.util.Map.of("created", created, "failed", failed, "total", data.size());
    }

}