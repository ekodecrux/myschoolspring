package com.myschool.backend.models.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

@Document(collection = "users")
public class User {

    @Id
    private String mongoId;

    @Indexed(unique = true)
    private String id;

    @Indexed(unique = true)
    private String email;

    @JsonIgnore
    @Field("password_hash")
    private String passwordHash;

    private String name;
    private String role;

    @Field("school_code")
    @Indexed
    private String schoolCode;

    @Field("teacher_code")
    @Indexed
    private String teacherCode;

    @Field("student_code")
    private String studentCode;

    @Field("mobile_number")
    @Indexed
    private String mobileNumber;

    private String address;
    private String city;
    private String state;

    @Field("postal_code")
    private String postalCode;

    @Field("father_name")
    private String fatherName;

    @Field("principal_name")
    private String principalName;

    @Field("class_name")
    private String className;

    @Field("section_name")
    private String sectionName;

    @Field("roll_number")
    private String rollNumber;

    @Field("school_name")
    private String schoolName;

    private Integer credits;
    private Boolean disabled;

    @Field("require_password_change")
    private Boolean requirePasswordChange;

    @Field("subscription_status")
    private String subscriptionStatus;

    @Field("last_subscription_plan")
    private String lastSubscriptionPlan;

    @Field("last_subscription_date")
    private String lastSubscriptionDate;

    @Field("created_at")
    private String createdAt;

    @Field("updated_at")
    private String updatedAt;

    @Field("last_login")
    private Instant lastLogin;

    @Field("added_by")
    private String addedBy;

    @Field("created_by")
    private String createdBy;

    @Field("registration_type")
    private String registrationType;

    public User() {}

    public String getMongoId() { return mongoId; }
    public void setMongoId(String mongoId) { this.mongoId = mongoId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getSchoolCode() { return schoolCode; }
    public void setSchoolCode(String schoolCode) { this.schoolCode = schoolCode; }
    public String getTeacherCode() { return teacherCode; }
    public void setTeacherCode(String teacherCode) { this.teacherCode = teacherCode; }
    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }
    public String getPrincipalName() { return principalName; }
    public void setPrincipalName(String principalName) { this.principalName = principalName; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public Boolean getDisabled() { return disabled; }
    public void setDisabled(Boolean disabled) { this.disabled = disabled; }
    public Boolean getRequirePasswordChange() { return requirePasswordChange; }
    public void setRequirePasswordChange(Boolean v) { this.requirePasswordChange = v; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public String getLastSubscriptionPlan() { return lastSubscriptionPlan; }
    public void setLastSubscriptionPlan(String v) { this.lastSubscriptionPlan = v; }
    public String getLastSubscriptionDate() { return lastSubscriptionDate; }
    public void setLastSubscriptionDate(String v) { this.lastSubscriptionDate = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public Instant getLastLogin() { return lastLogin; }
    public void setLastLogin(Instant lastLogin) { this.lastLogin = lastLogin; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getRegistrationType() { return registrationType; }
    public void setRegistrationType(String v) { this.registrationType = v; }

    public static UserBuilder builder() { return new UserBuilder(); }

    public static class UserBuilder {
        private String mongoId, id, email, passwordHash, name, role, schoolCode, teacherCode,
                studentCode, mobileNumber, address, city, state, postalCode, fatherName,
                principalName, className, sectionName, rollNumber, schoolName, subscriptionStatus,
                lastSubscriptionPlan, lastSubscriptionDate, createdAt, updatedAt, addedBy,
                createdBy, registrationType;
        private Integer credits;
        private Boolean disabled, requirePasswordChange;
        private Instant lastLogin;

        public UserBuilder id(String v) { this.id = v; return this; }
        public UserBuilder email(String v) { this.email = v; return this; }
        public UserBuilder passwordHash(String v) { this.passwordHash = v; return this; }
        public UserBuilder name(String v) { this.name = v; return this; }
        public UserBuilder role(String v) { this.role = v; return this; }
        public UserBuilder schoolCode(String v) { this.schoolCode = v; return this; }
        public UserBuilder teacherCode(String v) { this.teacherCode = v; return this; }
        public UserBuilder studentCode(String v) { this.studentCode = v; return this; }
        public UserBuilder mobileNumber(String v) { this.mobileNumber = v; return this; }
        public UserBuilder address(String v) { this.address = v; return this; }
        public UserBuilder city(String v) { this.city = v; return this; }
        public UserBuilder state(String v) { this.state = v; return this; }
        public UserBuilder postalCode(String v) { this.postalCode = v; return this; }
        public UserBuilder fatherName(String v) { this.fatherName = v; return this; }
        public UserBuilder principalName(String v) { this.principalName = v; return this; }
        public UserBuilder className(String v) { this.className = v; return this; }
        public UserBuilder sectionName(String v) { this.sectionName = v; return this; }
        public UserBuilder rollNumber(String v) { this.rollNumber = v; return this; }
        public UserBuilder schoolName(String v) { this.schoolName = v; return this; }
        public UserBuilder credits(Integer v) { this.credits = v; return this; }
        public UserBuilder disabled(Boolean v) { this.disabled = v; return this; }
        public UserBuilder requirePasswordChange(Boolean v) { this.requirePasswordChange = v; return this; }
        public UserBuilder subscriptionStatus(String v) { this.subscriptionStatus = v; return this; }
        public UserBuilder lastSubscriptionPlan(String v) { this.lastSubscriptionPlan = v; return this; }
        public UserBuilder lastSubscriptionDate(String v) { this.lastSubscriptionDate = v; return this; }
        public UserBuilder createdAt(String v) { this.createdAt = v; return this; }
        public UserBuilder updatedAt(String v) { this.updatedAt = v; return this; }
        public UserBuilder lastLogin(Instant v) { this.lastLogin = v; return this; }
        public UserBuilder addedBy(String v) { this.addedBy = v; return this; }
        public UserBuilder createdBy(String v) { this.createdBy = v; return this; }
        public UserBuilder registrationType(String v) { this.registrationType = v; return this; }

        public User build() {
            User u = new User();
            u.mongoId = this.mongoId; u.id = this.id; u.email = this.email;
            u.passwordHash = this.passwordHash; u.name = this.name; u.role = this.role;
            u.schoolCode = this.schoolCode; u.teacherCode = this.teacherCode;
            u.studentCode = this.studentCode; u.mobileNumber = this.mobileNumber;
            u.address = this.address; u.city = this.city; u.state = this.state;
            u.postalCode = this.postalCode; u.fatherName = this.fatherName;
            u.principalName = this.principalName; u.className = this.className;
            u.sectionName = this.sectionName; u.rollNumber = this.rollNumber;
            u.schoolName = this.schoolName; u.credits = this.credits;
            u.disabled = this.disabled; u.requirePasswordChange = this.requirePasswordChange;
            u.subscriptionStatus = this.subscriptionStatus;
            u.lastSubscriptionPlan = this.lastSubscriptionPlan;
            u.lastSubscriptionDate = this.lastSubscriptionDate;
            u.createdAt = this.createdAt; u.updatedAt = this.updatedAt;
            u.lastLogin = this.lastLogin; u.addedBy = this.addedBy;
            u.createdBy = this.createdBy; u.registrationType = this.registrationType;
            return u;
        }
    }
}
