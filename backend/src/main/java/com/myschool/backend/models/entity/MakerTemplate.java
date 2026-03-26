package com.myschool.backend.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "maker_templates")
public class MakerTemplate {

    @Id
    private String mongoId;

    private String id;

    @Field("user_id")
    private String userId;

    private String name;
    private String makerType;
    private String pageSize;
    private String canvasBg;
    private List<Map<String, Object>> elements;
    private Boolean isSystem;

    @Field("created_at")
    private String createdAt;

    @Field("updated_at")
    private String updatedAt;
}
