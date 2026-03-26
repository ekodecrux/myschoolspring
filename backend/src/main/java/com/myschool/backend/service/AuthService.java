package com.myschool.backend.service;

import com.myschool.backend.exception.AppException;
import com.myschool.backend.models.entity.*;
import com.myschool.backend.models.request.*;
import com.myschool.backend.models.response.LoginResponse;
import com.myschool.backend.repository.*;
import com.myschool.backend.security.JwtTokenProvider;
import com.myschool.backend.util.CodeGenerator;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private PasswordResetRepository passwordResetRepository;
    @Autowired private OtpSessionRepository otpSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private EmailService emailService;

    public Object login(LoginRequest request) {
        // Build query based on email and optional school code
        Optional<User> userOpt;
        if (request.getSchoolCode() != null && !request.getSchoolCode().isEmpty()) {
            userOpt = userRepository.findByEmailAndSchoolCode(request.getIdentifier(), request.getSchoolCode());
        } else {
            userOpt = userRepository.findByEmail(request.getIdentifier());
        }

        if (userOpt.isEmpty()) {
            throw new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        if (Boolean.TRUE.equals(user.getDisabled())) {
            throw new AppException("Account is disabled", HttpStatus.FORBIDDEN);
        }

        // Check if school admin is disabled (for teachers/students)
        if (user.getSchoolCode() != null &&
                (UserRole.TEACHER.equals(user.getRole()) || UserRole.STUDENT.equals(user.getRole()))) {
            Optional<User> schoolAdmin = userRepository.findFirstBySchoolCodeAndRole(user.getSchoolCode(), UserRole.SCHOOL_ADMIN);
            if (schoolAdmin.isPresent() && Boolean.TRUE.equals(schoolAdmin.get().getDisabled())) {
                throw new AppException("Your school is currently disabled. Please contact administrator.", HttpStatus.FORBIDDEN);
            }
        }

        // Check if school is active (for non-super-admins)
        if (user.getSchoolCode() != null && !UserRole.SUPER_ADMIN.equals(user.getRole())) {
            Optional<School> school = schoolRepository.findByCode(user.getSchoolCode());
            if (school.isPresent() && !Boolean.TRUE.equals(school.get().getIsActive())) {
                throw new AppException("Your school is currently inactive. Please contact administrator.", HttpStatus.FORBIDDEN);
            }
        }

        // Check if password change is required
        if (Boolean.TRUE.equals(user.getRequirePasswordChange())) {
            Map<String, Object> challengeData = new HashMap<>();
            challengeData.put("challengeName", "NEW_PASSWORD_REQUIRED");
            challengeData.put("session", UUID.randomUUID().toString());
            challengeData.put("username", request.getIdentifier());
            return Map.of(
                    "message", "Password change required",
                    "data", challengeData
            );
        }

        // Update last login
        user.setLastLogin(Instant.now());
        userRepository.save(user);

        // Get school info if applicable
        Map<String, Object> schoolInfo = null;
        if (user.getSchoolCode() != null) {
            Optional<School> school = schoolRepository.findByCode(user.getSchoolCode());
            if (school.isPresent()) {
                schoolInfo = new HashMap<>();
                schoolInfo.put("name", school.get().getName());
                schoolInfo.put("code", school.get().getCode());
            }
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getEmail(), user.getRole(), user.getSchoolCode());
        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(), user.getEmail(), user.getRole(), user.getSchoolCode());

        logger.info("Login successful - User ID: {}, Name: {}, Email: {}", user.getId(), user.getName(), user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message("Login successful")
                .school(schoolInfo)
                .build();
    }

    public Map<String, Object> register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException("Email already registered", HttpStatus.BAD_REQUEST);
        }

        // Auto-generate password if not provided
        String rawPassword = request.getPassword();
        boolean autoGenerated = false;
        if (rawPassword == null || rawPassword.isEmpty()) {
            rawPassword = CodeGenerator.generatePassword(12);
            autoGenerated = true;
        }

        String userId = UUID.randomUUID().toString();
        String role = request.getUserRole() != null ? request.getUserRole() : UserRole.INDIVIDUAL;

        // Generate role-specific codes
        String teacherCode = null;
        String studentCode = null;

        if (UserRole.TEACHER.equals(role)) {
            teacherCode = CodeGenerator.generateCode("TCH", 6);
        } else if (UserRole.STUDENT.equals(role)) {
            studentCode = CodeGenerator.generateCode("STU", 6);
        }

        User user = User.builder()
                .id(userId)
                .email(request.getEmail().toLowerCase())
                .name(request.getName())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .schoolCode(request.getSchoolCode())
                .teacherCode(request.getTeacherCode() != null ? request.getTeacherCode() : teacherCode)
                .studentCode(request.getStudentCode() != null ? request.getStudentCode() : studentCode)
                .mobileNumber(request.getMobileNumber())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .className(request.getClassName())
                .sectionName(request.getSectionName())
                .rollNumber(request.getRollNumber())
                .fatherName(request.getFatherName())
                .principalName(request.getPrincipalName())
                .credits(100)
                .disabled(false)
                .requirePasswordChange(autoGenerated)
                .subscriptionStatus("free")
                .createdAt(Instant.now().toString())
                .build();

        userRepository.save(user);

        // Send welcome email
        if (autoGenerated) {
            emailService.sendWelcomeEmail(request.getEmail(), request.getName(), rawPassword, role, "MySchool");
        } else {
            emailService.sendSelfRegistrationWelcomeEmail(request.getEmail(), request.getName(), role, "MySchool");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("message", "User registered successfully");
        result.put("email", request.getEmail());
        result.put("role", role);
        if (teacherCode != null) result.put("teacherCode", teacherCode);
        if (studentCode != null) result.put("studentCode", studentCode);

        return result;
    }

    public LoginResponse newPasswordChallenge(Map<String, Object> body) {
        String username = (String) body.get("username");
        String newPassword = (String) body.get("newPassword");

        if (username == null || newPassword == null) {
            throw new AppException("username and newPassword are required", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setRequirePasswordChange(false);
        userRepository.save(user);

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getEmail(), user.getRole(), user.getSchoolCode());
        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(), user.getEmail(), user.getRole(), user.getSchoolCode());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message("Password updated successfully")
                .build();
    }

    public Map<String, Object> refreshToken(Map<String, Object> body) {
        String refreshTok = (String) body.get("refreshToken");
        if (refreshTok == null) {
            throw new AppException("Refresh Token is required", HttpStatus.BAD_REQUEST);
        }

        Claims payload;
        try {
            payload = jwtTokenProvider.decodeToken(refreshTok);
        } catch (Exception e) {
            throw new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }

        if (!"refresh".equals(payload.get("type"))) {
            throw new AppException("Invalid token type", HttpStatus.UNAUTHORIZED);
        }

        String userId = (String) payload.get("userId");
        User user = userRepository.findByIdField(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getEmail(), user.getRole(), user.getSchoolCode());

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", refreshTok,
                "message", "Token refreshed successfully"
        );
    }

    public Map<String, Object> forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Map.of("message", "If the email exists, a reset code has been sent");
        }

        User user = userOpt.get();
        String resetCode = CodeGenerator.generateOtp();

        // Remove old codes
        passwordResetRepository.deleteByEmail(email);

        // Store new reset code
        PasswordReset reset = PasswordReset.builder()
                .email(email)
                .code(resetCode)
                .createdAt(Instant.now().toString())
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES).toString())
                .build();
        passwordResetRepository.save(reset);

        emailService.sendPasswordResetEmail(email, user.getName() != null ? user.getName() : "User", resetCode);

        return Map.of("message", "If the email exists, a reset code has been sent");
    }

    public Map<String, Object> confirmPassword(ConfirmPasswordResetRequest request) {
        PasswordReset reset = passwordResetRepository.findByEmailAndCode(request.getEmail(), request.getCode())
                .orElseThrow(() -> new AppException("Invalid or expired code", HttpStatus.BAD_REQUEST));

        Instant expiresAt = Instant.parse(reset.getExpiresAt());
        if (Instant.now().isAfter(expiresAt)) {
            passwordResetRepository.delete(reset);
            throw new AppException("Code has expired", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setRequirePasswordChange(false);
        userRepository.save(user);

        passwordResetRepository.delete(reset);

        return Map.of("message", "Password reset successfully");
    }

    public Map<String, Object> changePassword(ChangePasswordRequest request, User currentUser) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPasswordHash())) {
            throw new AppException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }

        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);

        return Map.of("message", "Password changed successfully");
    }

    public Map<String, Object> sendOtp(String phoneNumber) {
        String otp = CodeGenerator.generateOtp();
        String sessionId = UUID.randomUUID().toString();

        OtpSession session = OtpSession.builder()
                .sessionId(sessionId)
                .phoneNumber(phoneNumber)
                .otp(otp)
                .createdAt(Instant.now().toString())
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES).toString())
                .build();
        otpSessionRepository.save(session);

        logger.info("OTP for {}: {}", phoneNumber, otp);

        return Map.of(
                "message", "success",
                "sessionId", sessionId,
                "phoneNumber", phoneNumber
        );
    }

    public LoginResponse loginViaOtp(Map<String, Object> body) {
        String phoneNumber = (String) body.get("phoneNumber");
        String otp = (String) body.get("otp");

        OtpSession session = otpSessionRepository.findByPhoneNumberAndOtp(phoneNumber, otp)
                .orElseThrow(() -> new AppException("Invalid OTP", HttpStatus.UNAUTHORIZED));

        Instant expiresAt = Instant.parse(session.getExpiresAt());
        if (Instant.now().isAfter(expiresAt)) {
            throw new AppException("OTP expired", HttpStatus.UNAUTHORIZED);
        }

        Optional<User> userOpt = userRepository.findByMobileNumber(phoneNumber);
        User user;
        if (userOpt.isEmpty()) {
            String userId = UUID.randomUUID().toString();
            user = User.builder()
                    .id(userId)
                    .email(phoneNumber + "@myschool.temp")
                    .name("User")
                    .mobileNumber(phoneNumber)
                    .role(UserRole.INDIVIDUAL)
                    .credits(100)
                    .disabled(false)
                    .createdAt(Instant.now().toString())
                    .build();
            userRepository.save(user);
        } else {
            user = userOpt.get();
        }

        otpSessionRepository.delete(session);
        user.setLastLogin(Instant.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getEmail(), user.getRole(), user.getSchoolCode());
        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(), user.getEmail(), user.getRole(), user.getSchoolCode());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message("OTP Login successful")
                .build();
    }
}
