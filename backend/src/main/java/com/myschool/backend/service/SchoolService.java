package com.myschool.backend.service;

import com.myschool.backend.exception.AppException;
import com.myschool.backend.models.entity.School;
import com.myschool.backend.models.entity.User;
import com.myschool.backend.models.entity.UserRole;
import com.myschool.backend.models.request.CreateSchoolRequest;
import com.myschool.backend.repository.SchoolRepository;
import com.myschool.backend.repository.UserRepository;
import com.myschool.backend.util.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class SchoolService {

    private static final Logger logger = LoggerFactory.getLogger(SchoolService.class);

    @Autowired private SchoolRepository schoolRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;
    @Autowired private MongoTemplate mongoTemplate;

    public Map<String, Object> createSchool(CreateSchoolRequest request, User currentUser) {
        // Generate unique school code
        String schoolCode;
        do {
            schoolCode = CodeGenerator.generateCode("SCH", 6);
        } while (schoolRepository.existsByCode(schoolCode));

        // Check if admin email already exists
        if (userRepository.findByEmail(request.getAdminEmail()).isPresent()) {
            throw new AppException("Admin email already registered", HttpStatus.BAD_REQUEST);
        }

        // Generate auto password for School Admin
        String autoPassword = CodeGenerator.generatePassword(12);
        String adminId = UUID.randomUUID().toString();

        // Create School Admin user
        User admin = User.builder()
                .id(adminId)
                .email(request.getAdminEmail())
                .name(request.getAdminName())
                .passwordHash(passwordEncoder.encode(autoPassword))
                .mobileNumber(request.getAdminPhone())
                .role(UserRole.SCHOOL_ADMIN)
                .schoolCode(schoolCode)
                .credits(5000)
                .disabled(false)
                .requirePasswordChange(true)
                .createdAt(Instant.now().toString())
                .createdBy(currentUser.getId())
                .build();

        // Create school record
        School school = School.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .code(schoolCode)
                .principalName(request.getPrincipalName())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .adminId(adminId)
                .isActive(true)
                .createdAt(Instant.now().toString())
                .createdBy(currentUser.getId())
                .build();

        schoolRepository.save(school);
        userRepository.save(admin);

        // Send welcome email
        emailService.sendWelcomeEmail(
                request.getAdminEmail(),
                request.getAdminName(),
                autoPassword,
                "School Admin",
                request.getName()
        );

        return Map.of(
                "message", "School created successfully",
                "schoolCode", schoolCode,
                "adminEmail", request.getAdminEmail(),
                "note", "School Admin credentials have been sent to the email address"
        );
    }

    public Map<String, Object> listSchools(int limit, int skip) {
        List<School> allSchools = schoolRepository.findAll();
        long total = allSchools.size();

        List<School> paged = allSchools.stream()
                .skip(skip)
                .limit(limit)
                .toList();

        List<Map<String, Object>> schools = new ArrayList<>();
        for (School s : paged) {
            Map<String, Object> schoolMap = new HashMap<>();
            schoolMap.put("id", s.getId());
            schoolMap.put("schoolCode", s.getCode());
            schoolMap.put("name", s.getName());
            schoolMap.put("principalName", s.getPrincipalName());
            schoolMap.put("address", s.getAddress());
            schoolMap.put("city", s.getCity());
            schoolMap.put("state", s.getState());
            schoolMap.put("postalCode", s.getPostalCode());
            schoolMap.put("isActive", s.getIsActive());
            schoolMap.put("teachersEnrolled", userRepository.countBySchoolCodeAndRole(s.getCode(), UserRole.TEACHER));
            schoolMap.put("studentsEnrolled", userRepository.countBySchoolCodeAndRole(s.getCode(), UserRole.STUDENT));
            schoolMap.put("credits", s.getCredits() != null ? s.getCredits() : 0);
            schools.add(schoolMap);
        }

        return Map.of(
                "data", schools,
                "total", total,
                "limit", limit,
                "skip", skip
        );
    }

    public List<Map<String, Object>> getActiveSchoolsPublic() {
        List<School> activeSchools = schoolRepository.findAllActiveSchools();
        List<Map<String, Object>> result = new ArrayList<>();
        for (School s : activeSchools) {
            result.add(Map.of("code", s.getCode(), "name", s.getName()));
        }
        return result;
    }

    public Map<String, Object> getSchool(String schoolCode, User currentUser) {
        School school = schoolRepository.findByCode(schoolCode)
                .orElseThrow(() -> new AppException("School not found", HttpStatus.NOT_FOUND));

        Map<String, Object> schoolMap = new HashMap<>();
        schoolMap.put("id", school.getId());
        schoolMap.put("name", school.getName());
        schoolMap.put("code", school.getCode());
        schoolMap.put("principalName", school.getPrincipalName());
        schoolMap.put("address", school.getAddress());
        schoolMap.put("city", school.getCity());
        schoolMap.put("state", school.getState());
        schoolMap.put("postalCode", school.getPostalCode());
        schoolMap.put("isActive", school.getIsActive());

        // Get admin details
        if (school.getAdminId() != null) {
            userRepository.findByIdField(school.getAdminId()).ifPresent(admin -> {
                Map<String, Object> adminMap = new HashMap<>();
                adminMap.put("id", admin.getId());
                adminMap.put("email", admin.getEmail());
                adminMap.put("name", admin.getName());
                adminMap.put("mobileNumber", admin.getMobileNumber());
                schoolMap.put("admin", adminMap);
            });
        }

        // Get stats
        schoolMap.put("stats", Map.of(
                "teachers", userRepository.countBySchoolCodeAndRole(schoolCode, UserRole.TEACHER),
                "students", userRepository.countBySchoolCodeAndRole(schoolCode, UserRole.STUDENT),
                "parents", userRepository.countBySchoolCodeAndRole(schoolCode, UserRole.PARENT)
        ));

        return schoolMap;
    }

    public Map<String, Object> toggleSchoolStatus(String schoolCode) {
        School school = schoolRepository.findByCode(schoolCode)
                .orElseThrow(() -> new AppException("School not found", HttpStatus.NOT_FOUND));

        boolean newStatus = !Boolean.TRUE.equals(school.getIsActive());
        school.setIsActive(newStatus);
        school.setUpdatedAt(Instant.now().toString());
        schoolRepository.save(school);

        return Map.of("message", "School " + (newStatus ? "activated" : "deactivated") + " successfully");
    }
}
