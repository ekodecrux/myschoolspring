package com.myschool.backend.service;

import com.myschool.backend.exception.AppException;
import com.myschool.backend.models.entity.School;
import com.myschool.backend.models.entity.User;
import com.myschool.backend.models.entity.UserRole;
import com.myschool.backend.models.request.RegisterRequest;
import com.myschool.backend.repository.SchoolRepository;
import com.myschool.backend.repository.UserRepository;
import com.myschool.backend.util.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private AuthService authService;
    @Autowired private EmailService emailService;

    public Map<String, Object> getUserDetails(User currentUser) {
        User user = userRepository.findByIdField(currentUser.getId())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        Map<String, Object> result = buildUserResponse(user);

        // Get school name if user has school_code
        if (user.getSchoolCode() != null) {
            schoolRepository.findByCode(user.getSchoolCode()).ifPresent(school -> {
                result.put("school_name", school.getName());
                result.put("schoolName", school.getName());
            });
        }

        // Get teacher name if user has teacher_code
        if (user.getTeacherCode() != null && UserRole.STUDENT.equals(user.getRole())) {
            userRepository.findByTeacherCode(user.getTeacherCode()).ifPresent(teacher -> {
                result.put("teacher_name", teacher.getName());
                result.put("teacherName", teacher.getName());
            });
        }

        return result;
    }

    public Map<String, Object> updateUserDetails(Map<String, Object> body, User currentUser) {
        Set<String> allowedFields = Set.of("name", "mobile_number", "mobileNumber", "address", "city", "state", "postal_code", "postalCode");
        Map<String, Object> updateData = new HashMap<>();

        for (Map.Entry<String, Object> entry : body.entrySet()) {
            if (allowedFields.contains(entry.getKey()) && entry.getValue() != null) {
                updateData.put(entry.getKey(), entry.getValue());
            }
        }

        // Convert camelCase to snake_case
        if (updateData.containsKey("mobileNumber")) {
            updateData.put("mobile_number", updateData.remove("mobileNumber"));
        }
        if (updateData.containsKey("postalCode")) {
            updateData.put("postal_code", updateData.remove("postalCode"));
        }

        if (updateData.isEmpty()) {
            throw new AppException("No fields to update", HttpStatus.BAD_REQUEST);
        }

        Query query = new Query(Criteria.where("id").is(currentUser.getId()));
        Update update = new Update();
        updateData.forEach(update::set);
        update.set("updated_at", Instant.now().toString());
        mongoTemplate.updateFirst(query, update, User.class);

        return Map.of("message", "User details updated successfully");
    }

    public Map<String, Object> listUsers(String role, int limit, User currentUser) {
        String userRole = currentUser.getRole();

        if (!UserRole.isTeacherOrAbove(userRole)) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        Query query = new Query();

        if (role != null && !role.isEmpty()) {
            query.addCriteria(Criteria.where("role").is(role));
        }

        // Filter by school for non-super-admins
        if (!UserRole.SUPER_ADMIN.equals(userRole)) {
            query.addCriteria(Criteria.where("school_code").is(currentUser.getSchoolCode()));
        }

        // Teachers can only see their students
        if (UserRole.TEACHER.equals(userRole)) {
            query.addCriteria(Criteria.where("teacher_code").is(currentUser.getTeacherCode()));
        }

        query.limit(limit);
        query.fields().exclude("password_hash");

        List<User> users = mongoTemplate.find(query, User.class);

        // Build enriched user list
        List<Map<String, Object>> transformedUsers = users.stream()
                .map(this::buildUserListItem)
                .collect(Collectors.toList());

        return Map.of(
                "data", Map.of(
                        "users", transformedUsers,
                        "count", transformedUsers.size(),
                        "lastUserId", transformedUsers.isEmpty() ? null :
                                transformedUsers.get(transformedUsers.size() - 1).get("userId")
                )
        );
    }

    public Map<String, Object> searchUsers(String searchQuery, String role, int limit, User currentUser) {
        String userRole = currentUser.getRole();
        if (!UserRole.isTeacherOrAbove(userRole)) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        String escapedQuery = searchQuery.trim();
        List<Criteria> orConditions = new ArrayList<>();
        String[] searchFields = {"name", "email", "mobile_number", "school_code", "teacher_code",
                "student_code", "class_name", "section_name", "city", "state", "father_name", "principal_name", "address"};

        for (String field : searchFields) {
            orConditions.add(Criteria.where(field).regex(".*" + escapedQuery + ".*", "i"));
        }

        Criteria criteria = new Criteria().orOperator(orConditions.toArray(new Criteria[0]));
        Query query = new Query(criteria);

        if (role != null && !role.isEmpty()) {
            query.addCriteria(Criteria.where("role").is(role));
        }

        if (!UserRole.SUPER_ADMIN.equals(userRole)) {
            query.addCriteria(Criteria.where("school_code").is(currentUser.getSchoolCode()));
        }

        query.limit(limit);
        query.fields().exclude("password_hash");

        List<User> users = mongoTemplate.find(query, User.class);
        List<Map<String, Object>> transformedUsers = users.stream()
                .map(this::buildUserListItem)
                .collect(Collectors.toList());

        return Map.of("data", Map.of("users", transformedUsers, "count", transformedUsers.size()));
    }

    public Map<String, Object> addUser(RegisterRequest request, User currentUser) {
        String userRole = currentUser.getRole();

        if (!UserRole.isTeacherOrAbove(userRole)) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        // Validate mandatory fields based on role being added
        if (UserRole.STUDENT.equals(request.getUserRole())) {
            if (UserRole.SUPER_ADMIN.equals(userRole)) {
                if (request.getSchoolCode() == null || request.getSchoolCode().isEmpty()) {
                    throw new AppException("School Code is required for students", HttpStatus.BAD_REQUEST);
                }
                if (request.getTeacherCode() == null || request.getTeacherCode().isEmpty()) {
                    throw new AppException("Teacher Code is required for students", HttpStatus.BAD_REQUEST);
                }
                // Validate school code exists
                schoolRepository.findByCode(request.getSchoolCode().toUpperCase())
                        .orElseThrow(() -> new AppException("School with code '" + request.getSchoolCode() + "' not found", HttpStatus.BAD_REQUEST));
                // Validate teacher code exists
                userRepository.findByTeacherCode(request.getTeacherCode())
                        .orElseThrow(() -> new AppException("Teacher with code '" + request.getTeacherCode() + "' not found", HttpStatus.BAD_REQUEST));
            }
        }

        if (UserRole.TEACHER.equals(request.getUserRole())) {
            if (UserRole.SUPER_ADMIN.equals(userRole) &&
                    (request.getSchoolCode() == null || request.getSchoolCode().isEmpty())) {
                throw new AppException("School Code is required for teachers", HttpStatus.BAD_REQUEST);
            }
        }

        // Set school code from current user for non-super-admins
        if (!UserRole.SUPER_ADMIN.equals(userRole)) {
            request.setSchoolCode(currentUser.getSchoolCode());
        }

        // Permission checks
        if (UserRole.SCHOOL_ADMIN.equals(userRole)) {
            Set<String> allowedRoles = Set.of(UserRole.SCHOOL_ADMIN, UserRole.TEACHER, UserRole.STUDENT, UserRole.PARENT);
            if (!allowedRoles.contains(request.getUserRole())) {
                throw new AppException("Admins can only add school staff and students", HttpStatus.FORBIDDEN);
            }
        }

        if (UserRole.TEACHER.equals(userRole)) {
            if (!UserRole.STUDENT.equals(request.getUserRole())) {
                throw new AppException("Teachers can only add students", HttpStatus.FORBIDDEN);
            }
            request.setSchoolCode(currentUser.getSchoolCode());
            request.setTeacherCode(currentUser.getTeacherCode());
        }

        // Auto-generate password
        request.setPassword(null);

        Map<String, Object> result = authService.register(request);

        // Update added_by
        String newUserId = (String) result.get("userId");
        Query query = new Query(Criteria.where("id").is(newUserId));
        Update update = new Update().set("added_by", currentUser.getId());
        mongoTemplate.updateFirst(query, update, User.class);

        result.put("addedBy", currentUser.getId());
        return result;
    }

    public Map<String, Object> updateCredits(Map<String, Object> body, User currentUser) {
        String userRole = currentUser.getRole();

        if (!UserRole.isTeacherOrAbove(userRole)) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        String userId = (String) body.get("userId");
        Object creditsObj = body.get("credits");
        String action = body.getOrDefault("action", "set").toString();

        if (userId == null || creditsObj == null) {
            throw new AppException("userId and credits are required", HttpStatus.BAD_REQUEST);
        }

        int credits;
        try {
            credits = Integer.parseInt(creditsObj.toString());
            if (credits < 0) throw new AppException("Credits cannot be negative", HttpStatus.BAD_REQUEST);
        } catch (NumberFormatException e) {
            throw new AppException("Credits must be a valid number", HttpStatus.BAD_REQUEST);
        }

        User targetUser = userRepository.findByIdField(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        int currentCredits = targetUser.getCredits() != null ? targetUser.getCredits() : 0;

        // Super Admin has unlimited credits
        if (UserRole.SUPER_ADMIN.equals(userRole)) {
            int newCredits = calculateNewCredits(action, currentCredits, credits);
            if (newCredits < 0) {
                return Map.of("success", false,
                        "message", "Cannot remove " + credits + " credits. User only has " + currentCredits + " credits.",
                        "user_credits", currentCredits);
            }
            targetUser.setCredits(newCredits);
            userRepository.save(targetUser);
            return Map.of("success", true, "message", "Credits updated successfully",
                    "new_credits", newCredits, "admin_credits", -1);
        }

        // For School Admin or Teacher - credits are transferred from their account
        User adminUser = userRepository.findByIdField(currentUser.getId())
                .orElseThrow(() -> new AppException("Admin not found", HttpStatus.NOT_FOUND));
        int adminCredits = adminUser.getCredits() != null ? adminUser.getCredits() : 0;

        if ("add".equals(action)) {
            if (adminCredits < credits) {
                return Map.of("success", false,
                        "message", "Insufficient credits. You have " + adminCredits + " credits but trying to transfer " + credits + ".",
                        "admin_credits", adminCredits);
            }
            targetUser.setCredits(currentCredits + credits);
            adminUser.setCredits(adminCredits - credits);
        } else if ("remove".equals(action)) {
            int newCredits = currentCredits - credits;
            if (newCredits < 0) {
                return Map.of("success", false,
                        "message", "Cannot remove " + credits + " credits. User only has " + currentCredits + " credits.",
                        "admin_credits", adminCredits, "user_credits", currentCredits);
            }
            targetUser.setCredits(newCredits);
            adminUser.setCredits(adminCredits + credits);
        } else { // set
            int diff = credits - currentCredits;
            if (diff > 0 && adminCredits < diff) {
                return Map.of("success", false,
                        "message", "Insufficient credits. You have " + adminCredits + " credits but need " + diff + " more.",
                        "admin_credits", adminCredits);
            }
            targetUser.setCredits(credits);
            adminUser.setCredits(diff > 0 ? adminCredits - diff : adminCredits + Math.abs(diff));
        }

        userRepository.save(targetUser);
        userRepository.save(adminUser);

        return Map.of("success", true, "message", "Credits updated successfully",
                "new_credits", targetUser.getCredits(),
                "admin_credits", adminUser.getCredits());
    }

    public Map<String, Object> disableAccount(String userId, User currentUser) {
        if (!("SUPER_ADMIN".equals(currentUser.getRole()) || "SCHOOL_ADMIN".equals(currentUser.getRole()))) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        if (userId.equals(currentUser.getId())) {
            throw new AppException("Cannot disable your own account", HttpStatus.BAD_REQUEST);
        }

        User targetUser = userRepository.findByIdField(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        targetUser.setDisabled(!Boolean.TRUE.equals(targetUser.getDisabled()));
        userRepository.save(targetUser);

        return Map.of("message", "User " + (targetUser.getDisabled() ? "disabled" : "enabled") + " successfully");
    }

    private int calculateNewCredits(String action, int current, int credits) {
        return switch (action) {
            case "add" -> current + credits;
            case "remove" -> current - credits;
            default -> credits; // "set"
        };
    }

    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("id", user.getId());
        result.put("email", user.getEmail());
        result.put("emailId", user.getEmail());
        result.put("name", user.getName());
        result.put("role", user.getRole());
        result.put("schoolCode", user.getSchoolCode() != null ? user.getSchoolCode() : "");
        result.put("teacherCode", user.getTeacherCode() != null ? user.getTeacherCode() : "");
        result.put("studentCode", user.getStudentCode() != null ? user.getStudentCode() : "");
        result.put("mobileNumber", user.getMobileNumber() != null ? user.getMobileNumber() : "");
        result.put("postalCode", user.getPostalCode() != null ? user.getPostalCode() : "");
        result.put("className", user.getClassName() != null ? user.getClassName() : "");
        result.put("sectionName", user.getSectionName() != null ? user.getSectionName() : "");
        result.put("rollNumber", user.getRollNumber() != null ? user.getRollNumber() : "");
        result.put("fatherName", user.getFatherName() != null ? user.getFatherName() : "");
        result.put("principalName", user.getPrincipalName() != null ? user.getPrincipalName() : "");
        result.put("subscriptionStatus", user.getSubscriptionStatus() != null ? user.getSubscriptionStatus() : "free");
        result.put("credits", user.getCredits() != null ? user.getCredits() : 0);
        result.put("disabled", user.getDisabled() != null ? user.getDisabled() : false);
        result.put("address", user.getAddress());
        result.put("city", user.getCity());
        result.put("state", user.getState());
        result.put("createdAt", user.getCreatedAt());
        return result;
    }

    private Map<String, Object> buildUserListItem(User user) {
        Map<String, Object> item = buildUserResponse(user);
        item.put("schoolName", user.getSchoolName() != null ? user.getSchoolName() : "");
        item.put("teacherName", "");
        item.put("studentCount", 0);
        item.put("teachersEnrolled", 0);
        item.put("studentsEnrolled", 0);
        return item;
    }

    public Map<String, Object> adminUpdateUser(Map<String, Object> body, User currentUser) {
        String role = currentUser.getRole() != null ? currentUser.getRole().toString() : "";
        if (!role.equals("SUPER_ADMIN") && !role.equals("SCHOOL_ADMIN")) {
            throw new AppException("Only Super Admin or School Admin can update users", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        String userId = (String) body.get("userId");
        if (userId == null || userId.isEmpty()) {
            throw new AppException("userId is required", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        User target = userRepository.findByIdField(userId)
                .orElseThrow(() -> new AppException("User not found", org.springframework.http.HttpStatus.NOT_FOUND));
        if (role.equals("SCHOOL_ADMIN") && !currentUser.getSchoolCode().equals(target.getSchoolCode())) {
            throw new AppException("You can only update users in your school", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        if (body.containsKey("name") && body.get("name") != null) target.setName((String) body.get("name"));
        if (body.containsKey("email") && body.get("email") != null) target.setEmail((String) body.get("email"));
        if (body.containsKey("mobileNumber") && body.get("mobileNumber") != null) target.setMobileNumber((String) body.get("mobileNumber"));
        if (body.containsKey("mobile_number") && body.get("mobile_number") != null) target.setMobileNumber((String) body.get("mobile_number"));
        if (body.containsKey("city") && body.get("city") != null) target.setCity((String) body.get("city"));
        if (body.containsKey("state") && body.get("state") != null) target.setState((String) body.get("state"));
        if (body.containsKey("disabled") && body.get("disabled") != null) target.setDisabled((Boolean) body.get("disabled"));
        if (body.containsKey("credits") && body.get("credits") != null) {
            target.setCredits(((Number) body.get("credits")).intValue());
        }
        userRepository.save(target);
        return Map.of("message", "User updated successfully");
    }

    public Map<String, Object> checkCredits(User currentUser) {
        String role = currentUser.getRole() != null ? currentUser.getRole().toString() : "";
        if ("SUPER_ADMIN".equals(role)) {
            java.util.Map<String, Object> r = new java.util.HashMap<>();
            r.put("credits", -1); r.put("isUnlimited", true);
            r.put("subscriptionStatus", "unlimited"); r.put("needsSubscription", false);
            r.put("message", "Unlimited credits"); r.put("warningLevel", "none");
            r.put("canPurchase", false); r.put("canAccessSubscription", true);
            r.put("showCreditsInNavbar", true); r.put("currentPlan", "enterprise");
            return r;
        }
        int credits = currentUser.getCredits() != null ? currentUser.getCredits() : 0;
        String subscriptionStatus = currentUser.getSubscriptionStatus() != null ? currentUser.getSubscriptionStatus() : "free";
        String warningLevel = null;
        String warningMessage = null;
        if (credits <= 0) { warningLevel = "critical"; warningMessage = "No credits remaining."; }
        else if (credits <= 10) { warningLevel = "critical"; warningMessage = "Only " + credits + " credits remaining."; }
        else if (credits <= 25) { warningLevel = "warning"; warningMessage = "You have " + credits + " credits remaining."; }
        String currentPlan = null;
        if ("enterprise".equals(subscriptionStatus) || credits >= 2000) currentPlan = "enterprise";
        else if ("premium".equals(subscriptionStatus) || credits >= 500) currentPlan = "premium";
        else if ("basic".equals(subscriptionStatus) || credits >= 100) currentPlan = "basic";
        boolean canPurchase = "SCHOOL_ADMIN".equals(role);
        boolean canAccessSubscription = "SCHOOL_ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("credits", credits);
        result.put("isUnlimited", false);
        result.put("subscriptionStatus", subscriptionStatus);
        result.put("needsSubscription", credits <= 0);
        result.put("message", warningMessage != null ? warningMessage : "You have " + credits + " credits available");
        result.put("warningLevel", warningLevel);
        result.put("canPurchase", canPurchase);
        result.put("canAccessSubscription", canAccessSubscription);
        result.put("showCreditsInNavbar", true);
        result.put("currentPlan", currentPlan);
        return result;
    }

    public Map<String, Object> useCredits(Map<String, Object> body, User currentUser) {
        String role = currentUser.getRole() != null ? currentUser.getRole().toString() : "";
        if ("SUPER_ADMIN".equals(role)) {
            return Map.of("success", true, "message", "Credits used successfully", "creditsUsed", 0, "remainingCredits", -1, "needsSubscription", false);
        }
        int creditsToUse = body.containsKey("credits") ? ((Number) body.get("credits")).intValue() : 1;
        int current = currentUser.getCredits() != null ? currentUser.getCredits() : 0;
        if (current < creditsToUse) {
            throw new AppException("Insufficient credits. You have " + current + " credits.", org.springframework.http.HttpStatus.PAYMENT_REQUIRED);
        }
        int newCredits = current - creditsToUse;
        currentUser.setCredits(newCredits);
        userRepository.save(currentUser);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("message", "Credits used successfully");
        result.put("creditsUsed", creditsToUse);
        result.put("remainingCredits", newCredits);
        result.put("needsSubscription", newCredits <= 0);
        if (newCredits <= 10) result.put("warningMessage", "Only " + newCredits + " credits remaining.");
        return result;
    }

    public Map<String, Object> purchaseCredits(Map<String, Object> body, User currentUser) {
        String role = currentUser.getRole() != null ? currentUser.getRole().toString() : "";
        if (!"SCHOOL_ADMIN".equals(role)) {
            throw new AppException("Only School Admins can purchase credits", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        int creditsRequested = body.containsKey("credits") ? ((Number) body.get("credits")).intValue() : 0;
        if (creditsRequested <= 0) {
            throw new AppException("Credits must be positive", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        return Map.of(
            "success", true,
            "message", "Credit purchase request for " + creditsRequested + " credits has been submitted. Super Admin will review and approve."
        );
    }

    public Map<String, Object> getSubscriptionHistory(User currentUser) {
        java.util.List<org.bson.Document> orders = mongoTemplate.getDb()
            .getCollection("orders")
            .find(new org.bson.Document("user_id", currentUser.getId()))
            .sort(new org.bson.Document("created_at", -1))
            .limit(50)
            .into(new java.util.ArrayList<>());
        java.util.List<java.util.Map<String, Object>> history = new java.util.ArrayList<>();
        for (org.bson.Document order : orders) {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", order.getString("id"));
            item.put("planName", order.getString("plan_name"));
            item.put("credits", order.getInteger("credits", 0));
            Object amtObj = order.get("amount");
            double amt = 0.0;
            if (amtObj instanceof Number) amt = ((Number) amtObj).doubleValue();
            item.put("amount", amt);
            item.put("status", order.getString("status"));
            item.put("createdAt", order.getString("created_at"));
            history.add(item);
        }
        return Map.of("history", history, "total", history.size());
    }

    /**
     * List users filtered by role, optionally by schoolCode and search query.
     * Returns {"data": {"users": [...], "hasMore": false}} to match frontend Redux slice.
     */
    public Map<String, Object> listUsersByRole(String role, int limit, String schoolCode, String search, User currentUser) {
        String callerRole = currentUser.getRole() != null ? currentUser.getRole().toString() : "";
        org.springframework.data.mongodb.core.query.Query query =
                new org.springframework.data.mongodb.core.query.Query();

        if (role != null && !role.isEmpty()) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("role").is(role));
        }
        if (schoolCode != null && !schoolCode.isEmpty()) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("school_code").is(schoolCode));
        } else if ("SCHOOL_ADMIN".equals(callerRole) && currentUser.getSchoolCode() != null) {
            // School admins can only see their own school's users
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("school_code").is(currentUser.getSchoolCode()));
        }
        if (search != null && !search.isEmpty()) {
            org.springframework.data.mongodb.core.query.Criteria searchCriteria =
                    new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                        org.springframework.data.mongodb.core.query.Criteria.where("name").regex(search, "i"),
                        org.springframework.data.mongodb.core.query.Criteria.where("email").regex(search, "i"),
                        org.springframework.data.mongodb.core.query.Criteria.where("school_code").regex(search, "i")
                    );
            query.addCriteria(searchCriteria);
        }
        query.limit(limit + 1); // fetch one extra to determine hasMore

        java.util.List<User> users = mongoTemplate.find(query, User.class);
        boolean hasMore = users.size() > limit;
        if (hasMore) users = users.subList(0, limit);

        java.util.List<Map<String, Object>> userList = users.stream().map(u -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", u.getId());
            m.put("userId", u.getId());
            m.put("name", u.getName() != null ? u.getName() : "");
            m.put("email", u.getEmail() != null ? u.getEmail() : "");
            m.put("emailId", u.getEmail() != null ? u.getEmail() : "");
            m.put("role", u.getRole() != null ? u.getRole().toString() : "");
            m.put("schoolCode", u.getSchoolCode() != null ? u.getSchoolCode() : "");
            m.put("schoolName", u.getSchoolName() != null ? u.getSchoolName() : "");
            m.put("mobileNumber", u.getMobileNumber() != null ? u.getMobileNumber() : "");
            m.put("city", u.getCity());
            m.put("state", u.getState());
            m.put("address", u.getAddress());
            m.put("credits", u.getCredits() != null ? u.getCredits() : 0);
            m.put("subscriptionStatus", u.getSubscriptionStatus() != null ? u.getSubscriptionStatus() : "free");
            m.put("disabled", (u.getDisabled() != null && u.getDisabled()));
            m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            m.put("className", u.getClassName() != null ? u.getClassName() : "");
            m.put("sectionName", u.getSectionName() != null ? u.getSectionName() : "");
            m.put("rollNumber", u.getRollNumber() != null ? u.getRollNumber() : "");
            m.put("teacherCode", u.getTeacherCode() != null ? u.getTeacherCode() : "");
            m.put("studentCode", u.getStudentCode() != null ? u.getStudentCode() : "");
            m.put("fatherName", u.getFatherName() != null ? u.getFatherName() : "");
            m.put("principalName", u.getPrincipalName() != null ? u.getPrincipalName() : "");
            m.put("studentsEnrolled", 0);
            m.put("teachersEnrolled", 0);
            m.put("studentCount", 0);
            m.put("teacherName", "");
            return m;
        }).collect(java.util.stream.Collectors.toList());

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("users", userList);
        data.put("hasMore", hasMore);
        data.put("count", userList.size());

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("data", data);
        return result;
    }

}
