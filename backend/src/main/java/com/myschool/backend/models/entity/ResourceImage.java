package com.myschool.backend.models.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "resource_images")
@CompoundIndexes({
    @CompoundIndex(name = "folder_cat_status", def = "{'folder_path': 1, 'category': 1, 'status': 1}"),
    @CompoundIndex(name = "folder_cat_subcat_status", def = "{'folder_path': 1, 'category': 1, 'subcategory': 1, 'status': 1}")
})
public class ResourceImage {

    @Id
    private String mongoId;

    @Indexed(unique = true)
    private String id;

    @Indexed
    private String category;

    @Indexed
    private String subcategory;

    private String menu;

    @Field("sub_menu")
    private String subMenu;

    private String subject;

    @Field("sub_topic")
    private String subTopic;

    @Field("book_type")
    private String bookType;

    @Field("unit_lesson")
    private String unitLesson;

    @Field("file_type")
    private String fileType;

    @Field("admin_code")
    private String adminCode;

    @Field("meta_name")
    private String metaName;

    @Indexed
    private List<String> tags;

    @Field("s3_path")
    private String s3Path;

    @Field("folder_path")
    @Indexed
    private String folderPath;

    private String url;

    @Field("thumbnail_url")
    private String thumbnailUrl;

    private String title;
    private String description;

    @Field("source_type")
    private String sourceType;

    @Indexed
    private String status;

    @Field("uploaded_at")
    private String uploadedAt;

    @Field("uploaded_by")
    private String uploadedBy;

    @Field("approved_by")
    private String approvedBy;

    @Field("approved_at")
    private String approvedAt;

    @Field("rejected_by")
    private String rejectedBy;

    @Field("rejected_at")
    private String rejectedAt;

    @Field("rejection_reason")
    private String rejectionReason;

    @Field("school_code")
    private String schoolCode;
}
