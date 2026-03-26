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

        return Map.of(
                "totalUsers", totalUsers,
                "totalSchools", totalSchools,
                "totalOrders", totalOrders,
                "totalImages", totalImages,
                "pendingImages", pendingImages,
                "usersByRole", Map.of(
                        "superAdmins", superAdmins,
                        "schoolAdmins", schoolAdmins,
                        "teachers", teachers,
                        "students", students,
                        "parents", parents,
                        "individuals", individuals
                )
        );
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
}
