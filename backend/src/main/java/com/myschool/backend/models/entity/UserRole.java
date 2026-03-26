package com.myschool.backend.models.entity;

public class UserRole {
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String SCHOOL_ADMIN = "SCHOOL_ADMIN";
    public static final String TEACHER = "TEACHER";
    public static final String STUDENT = "STUDENT";
    public static final String PARENT = "PARENT";
    public static final String INDIVIDUAL = "INDIVIDUAL";
    public static final String PUBLICATION = "PUBLICATION";

    public static boolean isAdminOrAbove(String role) {
        return SUPER_ADMIN.equals(role) || SCHOOL_ADMIN.equals(role);
    }

    public static boolean isTeacherOrAbove(String role) {
        return SUPER_ADMIN.equals(role) || SCHOOL_ADMIN.equals(role) || TEACHER.equals(role);
    }

    public static boolean isSuperAdmin(String role) {
        return SUPER_ADMIN.equals(role);
    }
}
