package com.myschool.backend.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "my_images")
public class MyImage {

    @Id
    private String mongoId;

    @Indexed(unique = true)
    private String id;

    @Indexed
    @Field("user_id")
    private String userId;

    private String url;
    private String filename;
    private String category;
    private String subcategory;
    private List<String> tags;
    private String title;
    private String description;

    @Field("file_size")
    private Long fileSize;

    @Field("content_type")
    private String contentType;

    @Field("uploaded_at")
    private String uploadedAt;
}
