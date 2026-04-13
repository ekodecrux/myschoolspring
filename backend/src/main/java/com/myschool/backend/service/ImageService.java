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

        // List resource images with filtering and pagination 
    public Map<String, Object> listImages(
            String category, String subcategory, String menu, String subMenu,
            String subject, String subTopic, String bookType, String unitLesson,
            String fileType, String adminCode, String metaName, String tags,
            String status, String folderPath, int limit, int skip, User currentUser) {

        Query query = new Query();

        // Status filter - non-admins see only approved
        if (currentUser != null && "SUPER_ADMIN".equals(currentUser.getRole())) {
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

        // Upload image to R2 and save metadata 
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

        // List images from R2 folder structure 
    public Map<String, Object> listR2Images(String prefix) {
        List<Map<String, Object>> objects = storageService.listObjects(prefix != null ? prefix : "");
        return Map.of("data", objects, "total", objects.size());
    }

        // Get distinct filter values for image bank 
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

    public Map<String, Object> fetchImages(String folderPath, int imagesPerPage, String continuationToken, User currentUser) {
        try {
            com.mongodb.client.MongoCollection<org.bson.Document> col = mongoTemplate.getDb().getCollection("resource_images");
            org.bson.Document query = new org.bson.Document("status", "active");

            // Build query based on folderPath
            String[] parts = folderPath != null ? folderPath.split("/") : new String[0];
            List<String> validParts = new ArrayList<>();
            for (String p : parts) {
                if (p != null && !p.isEmpty() && !p.equalsIgnoreCase("thumbnails")) {
                    validParts.add(p);
                }
            }

            Map<String, String> categoryMap = new HashMap<>();
            categoryMap.put("ACADEMIC", "ACADEMIC");
            categoryMap.put("EARLYCAREER", "EARLY-CAREER");
            categoryMap.put("EARLY_CAREER", "EARLY-CAREER");
            categoryMap.put("EARLY-CAREER", "EARLY-CAREER");
            categoryMap.put("EDUTAINMENT", "EDUTAINMENT");
            categoryMap.put("PRINTRICH", "PRINT-RICH");
            categoryMap.put("PRINT_RICH", "PRINT-RICH");
            categoryMap.put("PRINT-RICH", "PRINT-RICH");
            categoryMap.put("MAKER", "MAKER");
            categoryMap.put("INFOHUB", "INFO-HUB");
            categoryMap.put("INFO_HUB", "INFO-HUB");
            categoryMap.put("INFO-HUB", "INFO-HUB");
            categoryMap.put("ONE_CLICK_RESOURCE_CENTRE", "ONE CLICK RESOURCE CENTER");
            categoryMap.put("ONE CLICK RESOURCE CENTRE", "ONE CLICK RESOURCE CENTER");
            categoryMap.put("ONE_CLICK_RESOURCE_CENTER", "ONE CLICK RESOURCE CENTER");
            categoryMap.put("ONE CLICK RESOURCE CENTER", "ONE CLICK RESOURCE CENTER");
            categoryMap.put("SECTIONS", "ONE CLICK RESOURCE CENTER");

            Map<String, String> menuNameMap = new HashMap<>();
            menuNameMap.put("image bank", "IMAGE BANK");
            menuNameMap.put("imagebank", "IMAGE BANK");
            menuNameMap.put("image-bank", "IMAGE BANK");
            menuNameMap.put("comics", "COMICS");
            menuNameMap.put("rhymes", "RHYMES");
            menuNameMap.put("visual worksheets", "VISUAL WORKSHEETS");
            menuNameMap.put("visual-worksheets", "VISUAL WORKSHEETS");
            menuNameMap.put("safety", "SAFETY");
            menuNameMap.put("value education", "VALUE EDUCATION");
            menuNameMap.put("value-education", "VALUE EDUCATION");
            menuNameMap.put("art lessons", "ART LESSONS");
            menuNameMap.put("art-lessons", "ART LESSONS");
            menuNameMap.put("craft lessons", "CRAFT LESSONS");
            menuNameMap.put("craft-lessons", "CRAFT LESSONS");
            menuNameMap.put("computer lessons", "COMPUTER LESSONS");
            menuNameMap.put("computer-lessons", "COMPUTER LESSONS");
            menuNameMap.put("flash cards", "FLASH CARDS");
            menuNameMap.put("flash-cards", "FLASH CARDS");
            menuNameMap.put("gk & science", "GK & SCIENCE");
            menuNameMap.put("gk science", "GK & SCIENCE");
            menuNameMap.put("gk-science", "GK & SCIENCE");
            menuNameMap.put("project charts", "PROJECT CHARTS");
            menuNameMap.put("project-charts", "PROJECT CHARTS");
            menuNameMap.put("puzzels & riddles", "PUZZELS & RIDDLES");
            menuNameMap.put("puzzles riddles", "PUZZELS & RIDDLES");
            menuNameMap.put("puzzles-riddles", "PUZZELS & RIDDLES");
            menuNameMap.put("pictorial stories", "PICTORIAL STORIES");
            menuNameMap.put("pictorial-stories", "PICTORIAL STORIES");
            menuNameMap.put("picture stories", "PICTORIAL STORIES");
            menuNameMap.put("picture-stories", "PICTORIAL STORIES");
            menuNameMap.put("moral stories", "MORAL STORIES");
            menuNameMap.put("moral-stories", "MORAL STORIES");
            menuNameMap.put("learn hand writing", "LEARN HAND WRITING");
            menuNameMap.put("learn-hand-writing", "LEARN HAND WRITING");
            menuNameMap.put("offers", "OFFERS");
            menuNameMap.put("donors", "DONORS");

            boolean isOneClickMenu = false;
            if (!validParts.isEmpty()) {
                String firstPartUpper = validParts.get(0).toUpperCase().replace("-", "_").replace(" ", "_");
                if (!"ACADEMIC".equals(firstPartUpper)) {
                    if (validParts.size() > 1) {
                        String secondPartLower = validParts.get(1).toLowerCase().replace("_", " ").replace("-", " ");
                        if (menuNameMap.containsKey(secondPartLower)) {
                            isOneClickMenu = true;
                        }
                    }
                }
            }

            if (isOneClickMenu) {
                query.append("category", "ONE CLICK RESOURCE CENTER");
                // CENTER category uses menu=WEBP and sub_menu=SECTION_NAME
                query.append("menu", "WEBP");
                String menuLower = validParts.get(1).toLowerCase().replace("_", " ").replace("-", " ");
                query.append("sub_menu", menuNameMap.getOrDefault(menuLower, validParts.get(1).toUpperCase()));
                if (validParts.size() > 2) {
                    query.append("subject", new org.bson.Document("$regex", "^" + validParts.get(2) + "$").append("$options", "i"));
                }
                if (validParts.size() > 3) {
                    query.append("book_type", new org.bson.Document("$regex", "^" + validParts.get(3) + "$").append("$options", "i"));
                }
            } else if (!validParts.isEmpty()) {
                String firstPart = validParts.get(0).toUpperCase().replace("-", "_").replace(" ", "_");
                
                if (Arrays.asList("ONE_CLICK_RESOURCE_CENTRE", "ONE_CLICK_RESOURCE_CENTER", "SECTIONS").contains(firstPart)) {
                    query.append("category", "ONE CLICK RESOURCE CENTER");
                    // CENTER category uses menu=WEBP and sub_menu=SECTION_NAME
                    query.append("menu", "WEBP");
                    if (validParts.size() > 1) {
                        String menuLower = validParts.get(1).toLowerCase().replace("_", " ");
                        query.append("sub_menu", menuNameMap.getOrDefault(menuLower, validParts.get(1).toUpperCase()));
                    }
                    if (validParts.size() > 2) {
                        query.append("subject", new org.bson.Document("$regex", "^" + validParts.get(2) + "$").append("$options", "i"));
                    }
                    if (validParts.size() > 3) {
                        query.append("book_type", new org.bson.Document("$regex", "^" + validParts.get(3) + "$").append("$options", "i"));
                    }
                } else if ("ACADEMIC".equals(firstPart) && validParts.size() > 1 && "IMAGE BANK".equals(validParts.get(1).toUpperCase().replace("_", " "))) {
                    query.append("folder_path", "ACADEMIC/IMAGE BANK");
                    if (validParts.size() > 2) {
                        query.append("category", new org.bson.Document("$regex", "^" + validParts.get(2) + "$").append("$options", "i"));
                    }
                    if (validParts.size() > 3) {
                        query.append("subcategory", new org.bson.Document("$regex", "^" + validParts.get(3) + "$").append("$options", "i"));
                    }
                    // Level 4+ (e.g. ACADEMIC/IMAGE BANK/ANIMALS/DOMESTIC ANIMALS/IMAGES)
                    // The 5th part (index 4) is just a folder name in R2 path (like "IMAGES"), not a stored field.
                    // Do NOT add it as a query filter - images are already fully identified by folder_path+category+subcategory.
                } else {
                    query.append("category", categoryMap.getOrDefault(firstPart, firstPart.replace("_", " ")));
                    if (validParts.size() > 1) {
                        String menuLower = validParts.get(1).toLowerCase().replace("_", " ").replace("-", " ");
                        String menuDb = menuNameMap.get(menuLower);
                        if (menuDb != null) {
                            query.append("menu", menuDb);
                        } else {
                            query.append("menu", new org.bson.Document("$regex", "^" + validParts.get(1) + "$").append("$options", "i"));
                        }
                    }
                    if (validParts.size() > 2) {
                        query.append("sub_menu", new org.bson.Document("$regex", "^" + validParts.get(2) + "$").append("$options", "i"));
                    }
                    if (validParts.size() > 3) {
                        query.append("subject", new org.bson.Document("$regex", "^" + validParts.get(3) + "$").append("$options", "i"));
                    }
                    if (validParts.size() > 4) {
                        query.append("book_type", new org.bson.Document("$regex", "^" + validParts.get(4) + "$").append("$options", "i"));
                    }
                    if (validParts.size() > 5) {
                        query.append("unit_lesson", new org.bson.Document("$regex", "^" + validParts.get(5) + "$").append("$options", "i"));
                    }
                }
            }

            List<String> filters = new ArrayList<>();
            String filterField = null;

            // Safely extract category - may be a plain String or a regex Document
            String queryCategory = null;
            if (query.get("category") instanceof String) {
                queryCategory = query.getString("category");
            } else if (query.get("category") instanceof org.bson.Document) {
                queryCategory = ((org.bson.Document) query.get("category")).getString("$regex");
                if (queryCategory != null) queryCategory = queryCategory.replace("^", "").replace("$", "");
            }
            String queryMenu = null;
            if (query.get("menu") instanceof String) {
                queryMenu = query.getString("menu");
            } else if (query.get("menu") instanceof org.bson.Document) {
                queryMenu = ((org.bson.Document) query.get("menu")).getString("$regex");
                if (queryMenu != null) queryMenu = queryMenu.replace("^", "").replace("$", "");
            }

            if ("ONE CLICK RESOURCE CENTER".equals(queryCategory) && queryMenu != null) {
                // CENTER category: menu=WEBP, sub_menu=section, subject=class level, book_type=type
                if (!query.containsKey("subject")) {
                    filterField = "subject";
                    org.bson.Document filterQuery = new org.bson.Document("category", queryCategory)
                        .append("menu", queryMenu)
                        .append("status", "active");
                    if (query.containsKey("sub_menu")) {
                        filterQuery.append("sub_menu", query.get("sub_menu"));
                    }
                    for (String val : col.distinct(filterField, filterQuery, String.class)) {
                        if (val != null && !val.isEmpty()) filters.add(val);
                    }
                } else if (!query.containsKey("book_type")) {
                    filterField = "book_type";
                    Object subjectVal = query.get("subject");
                    org.bson.Document filterQuery = new org.bson.Document("category", queryCategory)
                        .append("menu", queryMenu)
                        .append("status", "active");
                    if (query.containsKey("sub_menu")) {
                        filterQuery.append("sub_menu", query.get("sub_menu"));
                    }
                    filterQuery.append("subject", subjectVal);
                    for (String val : col.distinct(filterField, filterQuery, String.class)) {
                        if (val != null && !val.isEmpty()) filters.add(val);
                    }
                }
            } else if ("ACADEMIC/IMAGE BANK".equals(query.getString("folder_path")) || 
                      (queryMenu != null && queryMenu.toUpperCase().contains("IMAGE BANK"))) {
                
                org.bson.Document baseFilterQuery = new org.bson.Document("folder_path", "ACADEMIC/IMAGE BANK");
                
                if (!query.containsKey("sub_menu") && !query.containsKey("category")) {
                    // Level 1: show category filters (ANIMALS, BIRDS, etc.)
                    filterField = "category";
                    for (String val : col.distinct(filterField, baseFilterQuery, String.class)) {
                        if (val != null && !val.isEmpty()) filters.add(val);
                    }
                } else if ((query.containsKey("sub_menu") || query.containsKey("category")) && !query.containsKey("subcategory")) {
                    // Level 2: category is set, show subcategory filters (DOMESTIC ANIMALS, WILD ANIMALS, etc.)
                    Object catObj = query.containsKey("sub_menu") ? query.get("sub_menu") : query.get("category");
                    String catValue = null;
                    if (catObj instanceof String) {
                        catValue = (String) catObj;
                    } else if (catObj instanceof org.bson.Document) {
                        catValue = ((org.bson.Document) catObj).getString("$regex");
                        if (catValue != null) catValue = catValue.replace("^", "").replace("$", "");
                    }
                    
                    if (catValue != null) {
                        filterField = "subcategory";
                        org.bson.Document filterQuery = new org.bson.Document("folder_path", "ACADEMIC/IMAGE BANK")
                            .append("category", new org.bson.Document("$regex", "^" + catValue + "$").append("$options", "i"));
                        for (String val : col.distinct(filterField, filterQuery, String.class)) {
                            if (val != null && !val.isEmpty()) filters.add(val);
                        }
                    }
                }
                // Level 3+: subcategory is set - no more filters, images load directly
            }

            Collections.sort(filters);

            java.util.Map<String, String> listDict = new java.util.LinkedHashMap<>();
            col.find(query).limit(imagesPerPage)
                .forEach(doc -> {
                    String s3path = doc.getString("s3_path");
                    String url = doc.getString("url");
                    if (s3path == null || s3path.isEmpty()) s3path = url;
                    
                    if (s3path != null && !s3path.isEmpty()) {
                        String finalUrl = url;
                        if (finalUrl == null || finalUrl.isEmpty()) {
                            finalUrl = doc.getString("thumbnail_url");
                        }
                        if (finalUrl == null) finalUrl = "";
                        listDict.put(s3path, finalUrl);
                    }
                });

            long total = col.countDocuments(query);
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("list", listDict);
            result.put("continuationToken", null);
            result.put("isTruncated", listDict.size() >= imagesPerPage);
            result.put("totalCount", total);
            result.put("filters", filters);
            result.put("filterField", filterField);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            java.util.Map<String, Object> err = new java.util.LinkedHashMap<>();
            err.put("list", new java.util.LinkedHashMap<>());
            err.put("totalCount", 0L);
            err.put("isTruncated", false);
            err.put("filters", new ArrayList<>());
            err.put("filterField", null);
            return err;
        }
    }

    public Map<String, Object> getAcademicSubjects(String classLevel) {
        Map<String, Object> result = new HashMap<>();
        if (classLevel == null || classLevel.isEmpty()) {
            result.put("subjects", new ArrayList<>());
            return result;
        }
        try {
            com.mongodb.client.MongoCollection<org.bson.Document> col = mongoTemplate.getDb().getCollection("resource_images");
            org.bson.Document query = new org.bson.Document("category", "ACADEMIC")
                    .append("menu", "CLASS")
                    .append("sub_menu", classLevel)
                    .append("status", "active");
            List<String> subjects = col.distinct("subject", query, String.class)
                    .into(new ArrayList<>());
            subjects.removeIf(s -> s == null || s.isEmpty());
            Collections.sort(subjects);
            result.put("subjects", subjects);
        } catch (Exception e) {
            logger.error("Error fetching subjects for {}: {}", classLevel, e.getMessage());
            result.put("subjects", new ArrayList<>());
        }
        return result;
    }

    public Map<String, Object> getAcademicBookTypes(String classLevel, String subject) {
        Map<String, Object> result = new HashMap<>();
        if (classLevel == null || classLevel.isEmpty() || subject == null || subject.isEmpty()) {
            result.put("book_types", new ArrayList<>());
            return result;
        }
        try {
            com.mongodb.client.MongoCollection<org.bson.Document> col = mongoTemplate.getDb().getCollection("resource_images");
            org.bson.Document query = new org.bson.Document("category", "ACADEMIC")
                    .append("menu", "CLASS")
                    .append("sub_menu", classLevel)
                    .append("subject", subject)
                    .append("status", "active");
            List<String> bookTypes = col.distinct("book_type", query, String.class)
                    .into(new ArrayList<>());
            bookTypes.removeIf(bt -> bt == null || bt.isEmpty());
            Collections.sort(bookTypes);
            result.put("book_types", bookTypes);
        } catch (Exception e) {
            logger.error("Error fetching book types for {}/{}: {}", classLevel, subject, e.getMessage());
            result.put("book_types", new ArrayList<>());
        }
        return result;
    }

    public Map<String, Object> getAcademicUnitLessons(String classLevel, String subject, String bookType) {
        Map<String, Object> result = new HashMap<>();
        if (classLevel == null || classLevel.isEmpty() || subject == null || subject.isEmpty() || bookType == null || bookType.isEmpty()) {
            result.put("unit_lessons", new ArrayList<>());
            return result;
        }
        try {
            com.mongodb.client.MongoCollection<org.bson.Document> col = mongoTemplate.getDb().getCollection("resource_images");
            org.bson.Document query = new org.bson.Document("category", "ACADEMIC")
                    .append("menu", "CLASS")
                    .append("sub_menu", classLevel)
                    .append("subject", subject)
                    .append("book_type", bookType)
                    .append("status", "active");
            List<String> unitLessons = col.distinct("unit_lesson", query, String.class)
                    .into(new ArrayList<>());
            unitLessons.removeIf(ul -> ul == null || ul.isEmpty());
            result.put("unit_lessons", unitLessons);
        } catch (Exception e) {
            logger.error("Error fetching unit lessons for {}/{}/{}: {}", classLevel, subject, bookType, e.getMessage());
            result.put("unit_lessons", new ArrayList<>());
        }
        return result;
    }
}
