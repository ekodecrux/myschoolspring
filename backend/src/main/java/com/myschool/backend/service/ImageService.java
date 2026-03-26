package com.myschool.backend.service;

import com.myschool.backend.exception.AppException;
import com.myschool.backend.models.entity.MyImage;
import com.myschool.backend.models.entity.ResourceImage;
import com.myschool.backend.models.entity.User;
import com.myschool.backend.models.entity.UserRole;
import com.myschool.backend.repository.MyImageRepository;
import com.myschool.backend.repository.ResourceImageRepository;
import com.myschool.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Autowired private ResourceImageRepository resourceImageRepository;
    @Autowired private MyImageRepository myImageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StorageService storageService;
    @Autowired private MongoTemplate mongoTemplate;

        // List resource images with filtering and pagination (mirrors FastAPI /images/list)
    public Map<String, Object> listImages(
            String category, String subcategory, String menu, String subMenu,
            String subject, String subTopic, String bookType, String unitLesson,
            String fileType, String adminCode, String metaName, String tags,
            String status, String folderPath, int limit, int skip, User currentUser) {

        Query query = new Query();

        // Status filter - non-admins see only approved
        if ("SUPER_ADMIN".equals(currentUser.getRole())) {
            if (status != null && !status.isEmpty()) {
                query.addCriteria(Criteria.where("status").is(status));
            }
        } else {
            query.addCriteria(Criteria.where("status").is("approved"));
        }

        if (category != null && !category.isEmpty())
            query.addCriteria(Criteria.where("category").is(category));
        if (subcategory != null && !subcategory.isEmpty())
            query.addCriteria(Criteria.where("subcategory").is(subcategory));
        if (menu != null && !menu.isEmpty())
            query.addCriteria(Criteria.where("menu").is(menu));
        if (subMenu != null && !subMenu.isEmpty())
            query.addCriteria(Criteria.where("sub_menu").is(subMenu));
        if (subject != null && !subject.isEmpty())
            query.addCriteria(Criteria.where("subject").is(subject));
        if (subTopic != null && !subTopic.isEmpty())
            query.addCriteria(Criteria.where("sub_topic").is(subTopic));
        if (bookType != null && !bookType.isEmpty())
            query.addCriteria(Criteria.where("book_type").is(bookType));
        if (unitLesson != null && !unitLesson.isEmpty())
            query.addCriteria(Criteria.where("unit_lesson").is(unitLesson));
        if (fileType != null && !fileType.isEmpty())
            query.addCriteria(Criteria.where("file_type").is(fileType));
        if (adminCode != null && !adminCode.isEmpty())
            query.addCriteria(Criteria.where("admin_code").is(adminCode));
        if (metaName != null && !metaName.isEmpty())
            query.addCriteria(Criteria.where("meta_name").is(metaName));
        if (tags != null && !tags.isEmpty()) {
            List<String> tagList = Arrays.asList(tags.split(","));
            query.addCriteria(Criteria.where("tags").in(tagList));
        }
        if (folderPath != null && !folderPath.isEmpty())
            query.addCriteria(Criteria.where("folder_path").is(folderPath));

        long total = mongoTemplate.count(query, ResourceImage.class);

        query.skip(skip).limit(limit);
        query.with(Sort.by(Sort.Direction.DESC, "_id"));

        List<ResourceImage> images = mongoTemplate.find(query, ResourceImage.class);

        return Map.of(
                "data", images.stream().map(this::buildImageResponse).collect(Collectors.toList()),
                "total", total,
                "limit", limit,
                "skip", skip
        );
    }

        // Upload image to R2 and save metadata (mirrors FastAPI /images/upload)
    public Map<String, Object> uploadImage(
            MultipartFile file, String category, String subcategory, String menu,
            String subMenu, String subject, String subTopic, String bookType,
            String unitLesson, String adminCode, String metaName, String tags,
            String title, String description, String folderPath, User currentUser) {

        if (file == null || file.isEmpty()) {
            throw new AppException("No file provided", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase()
                : "jpg";

        String fileType = determineFileType(contentType, extension);

        // Build S3 key
        String safeCategory = (category != null ? category : "general").replaceAll("[^a-zA-Z0-9_-]", "_");
        String safeSubcategory = (subcategory != null ? subcategory : "general").replaceAll("[^a-zA-Z0-9_-]", "_");
        String imageId = UUID.randomUUID().toString();
        String s3Key;

        if (folderPath != null && !folderPath.isEmpty()) {
            s3Key = folderPath + "/" + imageId + "." + extension;
        } else {
            s3Key = "images/" + safeCategory + "/" + safeSubcategory + "/" + imageId + "." + extension;
        }

        String imageUrl;
        try {
            imageUrl = storageService.uploadObject(s3Key, file.getBytes(), contentType);
        } catch (Exception e) {
            throw new AppException("Failed to upload file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Determine status
        String status = "SUPER_ADMIN".equals(currentUser.getRole()) ? "approved" : "pending";

        // Parse tags
        List<String> tagList = new ArrayList<>();
        if (tags != null && !tags.isEmpty()) {
            tagList = Arrays.asList(tags.split(","));
        }

        ResourceImage image = ResourceImage.builder()
                .id(imageId)
                .category(category)
                .subcategory(subcategory)
                .menu(menu)
                .subMenu(subMenu)
                .subject(subject)
                .subTopic(subTopic)
                .bookType(bookType)
                .unitLesson(unitLesson)
                .fileType(fileType)
                .adminCode(adminCode)
                .metaName(metaName)
                .tags(tagList)
                .s3Path(s3Key)
                .folderPath(folderPath)
                .url(imageUrl)
                .title(title)
                .description(description)
                .status(status)
                .uploadedAt(Instant.now().toString())
                .uploadedBy(currentUser.getId())
                .schoolCode(currentUser.getSchoolCode())
                .build();

        resourceImageRepository.save(image);

        return Map.of(
                "message", "Image uploaded successfully",
                "imageId", imageId,
                "url", imageUrl,
                "status", status
        );
    }

        // Approve or reject an image (Super Admin only)
    public Map<String, Object> approveRejectImage(String imageId, Map<String, Object> body, User currentUser) {
        if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        String action = (String) body.get("action");
        String rejectionReason = (String) body.get("rejectionReason");

        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw new AppException("Action must be 'approve' or 'reject'", HttpStatus.BAD_REQUEST);
        }

        Query query = new Query(Criteria.where("id").is(imageId));
        Update update = new Update();

        if ("approve".equals(action)) {
            update.set("status", "approved");
            update.set("approved_by", currentUser.getId());
            update.set("approved_at", Instant.now().toString());
        } else {
            update.set("status", "rejected");
            update.set("rejected_by", currentUser.getId());
            update.set("rejected_at", Instant.now().toString());
            if (rejectionReason != null) update.set("rejection_reason", rejectionReason);
        }

        mongoTemplate.updateFirst(query, update, ResourceImage.class);

        return Map.of("message", "Image " + action + "d successfully");
    }

        // Delete a resource image
    public Map<String, Object> deleteImage(String imageId, User currentUser) {
        if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        ResourceImage image = resourceImageRepository.findByImageId(imageId)
                .orElseThrow(() -> new AppException("Image not found", HttpStatus.NOT_FOUND));

        // Delete from R2
        if (image.getS3Path() != null) {
            storageService.deleteObject(image.getS3Path());
        }

        resourceImageRepository.delete(image);
        return Map.of("message", "Image deleted successfully");
    }

        // List user's own uploaded images (My Images)
    public Map<String, Object> listMyImages(User currentUser) {
        List<MyImage> images = myImageRepository.findByUserId(currentUser.getId());
        return Map.of(
                "data", images.stream().map(this::buildMyImageResponse).collect(Collectors.toList()),
                "total", images.size()
        );
    }

        // Upload to user's personal image library
    public Map<String, Object> uploadMyImage(MultipartFile file, String category, String tags,
                                              String title, User currentUser) {
        if (file == null || file.isEmpty()) {
            throw new AppException("No file provided", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase()
                : "jpg";

        String imageId = UUID.randomUUID().toString();
        String s3Key = "my-images/" + currentUser.getId() + "/" + imageId + "." + extension;

        String imageUrl;
        try {
            imageUrl = storageService.uploadObject(s3Key, file.getBytes(), contentType);
        } catch (Exception e) {
            throw new AppException("Failed to upload file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<String> tagList = new ArrayList<>();
        if (tags != null && !tags.isEmpty()) {
            tagList = Arrays.asList(tags.split(","));
        }

        MyImage image = MyImage.builder()
                .id(imageId)
                .userId(currentUser.getId())
                .url(imageUrl)
                .filename(originalFilename)
                .category(category)
                .tags(tagList)
                .title(title)
                .fileSize(file.getSize())
                .contentType(contentType)
                .uploadedAt(Instant.now().toString())
                .build();

        myImageRepository.save(image);

        return Map.of(
                "message", "Image uploaded successfully",
                "imageId", imageId,
                "url", imageUrl
        );
    }

        // Delete user's own image
    public Map<String, Object> deleteMyImage(String imageId, User currentUser) {
        MyImage image = myImageRepository.findByIdAndUserId(imageId, currentUser.getId())
                .orElseThrow(() -> new AppException("Image not found", HttpStatus.NOT_FOUND));

        // Extract S3 key from URL
        String baseUrl = storageService.getBaseUrl();
        String url = image.getUrl();
        if (url != null && baseUrl != null && url.startsWith(baseUrl)) {
            String s3Key = url.substring(baseUrl.length() + 1);
            storageService.deleteObject(s3Key);
        }

        myImageRepository.delete(image);
        return Map.of("message", "Image deleted successfully");
    }

        // List images from R2 folder structure (mirrors FastAPI /images/r2/list)
    public Map<String, Object> listR2Images(String prefix) {
        List<Map<String, Object>> objects = storageService.listObjects(prefix != null ? prefix : "");
        return Map.of("data", objects, "total", objects.size());
    }

        // Get distinct filter values for image bank (mirrors FastAPI /images/filters)
    public Map<String, Object> getImageFilters(String category, String subcategory) {
        Query query = new Query(Criteria.where("status").is("approved"));
        if (category != null && !category.isEmpty())
            query.addCriteria(Criteria.where("category").is(category));
        if (subcategory != null && !subcategory.isEmpty())
            query.addCriteria(Criteria.where("subcategory").is(subcategory));

        List<String> categories = mongoTemplate.findDistinct(new Query(), "category", ResourceImage.class, String.class);
        List<String> subcategories = mongoTemplate.findDistinct(query, "subcategory", ResourceImage.class, String.class);
        List<String> menus = mongoTemplate.findDistinct(query, "menu", ResourceImage.class, String.class);
        List<String> subjects = mongoTemplate.findDistinct(query, "subject", ResourceImage.class, String.class);

        return Map.of(
                "categories", categories.stream().filter(Objects::nonNull).collect(Collectors.toList()),
                "subcategories", subcategories.stream().filter(Objects::nonNull).collect(Collectors.toList()),
                "menus", menus.stream().filter(Objects::nonNull).collect(Collectors.toList()),
                "subjects", subjects.stream().filter(Objects::nonNull).collect(Collectors.toList())
        );
    }

    private String determineFileType(String contentType, String extension) {
        if (contentType != null) {
            if (contentType.startsWith("image/")) return "image";
            if (contentType.equals("application/pdf")) return "pdf";
            if (contentType.contains("video")) return "video";
        }
        return switch (extension) {
            case "pdf" -> "pdf";
            case "mp4", "avi", "mov", "mkv" -> "video";
            default -> "image";
        };
    }

    private Map<String, Object> buildImageResponse(ResourceImage img) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", img.getId());
        map.put("category", img.getCategory());
        map.put("subcategory", img.getSubcategory());
        map.put("menu", img.getMenu());
        map.put("subMenu", img.getSubMenu());
        map.put("subject", img.getSubject());
        map.put("subTopic", img.getSubTopic());
        map.put("bookType", img.getBookType());
        map.put("unitLesson", img.getUnitLesson());
        map.put("fileType", img.getFileType());
        map.put("adminCode", img.getAdminCode());
        map.put("metaName", img.getMetaName());
        map.put("tags", img.getTags());
        String imgUrl = img.getUrl() != null ? img.getUrl() : "";
        if (!imgUrl.startsWith("http") && !imgUrl.isEmpty()) {
            imgUrl = storageService.getPublicUrl(imgUrl);
        }
        map.put("url", imgUrl);
        map.put("imageUrl", imgUrl);
        String thumbUrl = img.getThumbnailUrl() != null ? img.getThumbnailUrl() : "";
        if (!thumbUrl.startsWith("http") && !thumbUrl.isEmpty()) {
            thumbUrl = storageService.getPublicUrl(thumbUrl);
        }
        map.put("thumbnailUrl", thumbUrl);

        map.put("title", img.getTitle());
        map.put("description", img.getDescription());
        map.put("status", img.getStatus());
        map.put("uploadedAt", img.getUploadedAt());
        map.put("folderPath", img.getFolderPath());
        return map;
    }

    private Map<String, Object> buildMyImageResponse(MyImage img) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", img.getId());
        map.put("url", img.getUrl());
        map.put("filename", img.getFilename());
        map.put("category", img.getCategory());
        map.put("tags", img.getTags());
        map.put("title", img.getTitle());
        map.put("uploadedAt", img.getUploadedAt());
        return map;
    }

    public byte[] downloadImageFromR2(String key) {
        try {
            String publicUrl = storageService.getPublicUrl(key);
            java.net.URL url = new java.net.URL(publicUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            try (java.io.InputStream is = conn.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to download image: " + e.getMessage());
        }
    }

    public byte[] getPdfThumbnail(String key) {
        // Return a placeholder - full PDF rendering requires additional libraries
        // For now redirect to the R2 URL which the frontend can handle
        throw new RuntimeException("PDF thumbnail not available - use direct R2 URL");
    }
}
