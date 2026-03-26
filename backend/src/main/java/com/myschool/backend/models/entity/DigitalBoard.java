package com.myschool.backend.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "digital_boards")
public class DigitalBoard {

    @Id
    private String mongoId;

    private String id;

    @Field("user_id")
    private String userId;

    private String name;
    private String data;
    private String thumbnail;

    @Field("created_at")
    private String createdAt;

    @Field("updated_at")
    private String updatedAt;
}
