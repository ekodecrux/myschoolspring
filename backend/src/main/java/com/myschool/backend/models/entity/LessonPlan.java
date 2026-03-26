package com.myschool.backend.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "lesson_plans")
public class LessonPlan {

    @Id
    private String mongoId;

    private String id;

    @Field("user_id")
    private String userId;

    private String title;
    private String subject;

    @Field("class_name")
    private String className;

    private String date;
    private String objectives;
    private String content;
    private String activities;
    private String resources;
    private String homework;
    private String notes;

    @Field("use_digital_board")
    private Boolean useDigitalBoard;

    @Field("created_at")
    private String createdAt;

    @Field("updated_at")
    private String updatedAt;
}
