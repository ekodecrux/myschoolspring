package com.myschool.backend.controller;

import com.myschool.backend.models.entity.User;
import com.myschool.backend.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequestMapping("/api/rest/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private WebClient.Builder webClientBuilder;

        // List resource images with filters and pagination
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listImages(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory,
            @RequestParam(required = false) String menu,
            @RequestParam(required = false) String subMenu,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String subTopic,
            @RequestParam(required = false) String bookType,
            @RequestParam(required = false) String unitLesson,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String adminCode,
            @RequestParam(required = false) String metaName,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String folderPath,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int skip,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = imageService.listImages(
                category, subcategory, menu, subMenu, subject, subTopic,
                bookType, unitLesson, fileType, adminCode, metaName, tags,
                status, folderPath, limit, skip, currentUser);
        return ResponseEntity.ok(result);
    }

        // Upload a resource image to R2
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory,
            @RequestParam(required = false) String menu,
            @RequestParam(required = false) String subMenu,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String subTopic,
            @RequestParam(required = false) String bookType,
            @RequestParam(required = false) String unitLesson,
            @RequestParam(required = false) String adminCode,
            @RequestParam(required = false) String metaName,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String folderPath,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = imageService.uploadImage(
                file, category, subcategory, menu, subMenu, subject, subTopic,
                bookType, unitLesson, adminCode, metaName, tags, title, description,
                folderPath, currentUser);
        return ResponseEntity.ok(result);
    }

        // Approve or reject an image (Super Admin only)
    @PostMapping("/{imageId}/approve")
    public ResponseEntity<Map<String, Object>> approveRejectImage(
            @PathVariable String imageId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = imageService.approveRejectImage(imageId, body, currentUser);
        return ResponseEntity.ok(result);
    }

        // Delete a resource image (Super Admin only)
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Map<String, Object>> deleteImage(
            @PathVariable String imageId,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = imageService.deleteImage(imageId, currentUser);
        return ResponseEntity.ok(result);
    }

        // List user's personal images
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> listMyImages(@AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = imageService.listMyImages(currentUser);
        return ResponseEntity.ok(result);
    }

        // Upload to user's personal image library
    @PostMapping("/my/upload")
    public ResponseEntity<Map<String, Object>> uploadMyImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String title,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = imageService.uploadMyImage(file, category, tags, title, currentUser);
        return ResponseEntity.ok(result);
    }

        // Delete user's own image
    @DeleteMapping("/my/{imageId}")
    public ResponseEntity<Map<String, Object>> deleteMyImage(
            @PathVariable String imageId,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = imageService.deleteMyImage(imageId, currentUser);
        return ResponseEntity.ok(result);
    }

        // List images from R2 folder structure
    @GetMapping("/r2/list")
    public ResponseEntity<Map<String, Object>> listR2Images(
            @RequestParam(required = false) String prefix) {
        Map<String, Object> result = imageService.listR2Images(prefix);
        return ResponseEntity.ok(result);
    }

        // Get distinct filter values for image bank
    @GetMapping("/filters")
    public ResponseEntity<Map<String, Object>> getImageFilters(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory) {
        Map<String, Object> result = imageService.getImageFilters(category, subcategory);
        return ResponseEntity.ok(result);
    }

        // Proxy image requests to avoid CORS issues
    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        try {
            byte[] imageBytes = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            return ResponseEntity.ok()
                    .header("Content-Type", "image/jpeg")
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Approve a pending image (admin route matching FastAPI /admin/approveImage)
    @PostMapping("/admin/approveImage")
    public ResponseEntity<Map<String, Object>> approveImageAdmin(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        String imageId = (String) body.get("imageId");
        if (imageId == null) {
            return ResponseEntity.badRequest().body(Map.of("detail", "imageId is required"));
        }
        Map<String, Object> approveBody = new java.util.HashMap<>();
        approveBody.put("action", "approve");
        Map<String, Object> result = imageService.approveRejectImage(imageId, approveBody, currentUser);
        return ResponseEntity.ok(result);
    }

    // Reject a pending image (admin route matching FastAPI /admin/rejectImage)
    @PostMapping("/admin/rejectImage")
    public ResponseEntity<Map<String, Object>> rejectImageAdmin(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        String imageId = (String) body.get("imageId");
        if (imageId == null) {
            return ResponseEntity.badRequest().body(Map.of("detail", "imageId is required"));
        }
        Map<String, Object> rejectBody = new java.util.HashMap<>(body);
        rejectBody.put("action", "reject");
        Map<String, Object> result = imageService.approveRejectImage(imageId, rejectBody, currentUser);
        return ResponseEntity.ok(result);
    }

    // Download image by key from R2
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadImage(
            @RequestParam String key,
            @AuthenticationPrincipal User currentUser) {
        try {
            byte[] data = imageService.downloadImageFromR2(key);
            String filename = key.contains("/") ? key.substring(key.lastIndexOf("/") + 1) : key;
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .header("Content-Type", "application/octet-stream")
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Generate PDF thumbnail (returns image bytes)
    @GetMapping("/pdf-thumbnail")
    public ResponseEntity<byte[]> getPdfThumbnail(
            @RequestParam String key) {
        try {
            byte[] thumbnail = imageService.getPdfThumbnail(key);
            return ResponseEntity.ok()
                    .header("Content-Type", "image/jpeg")
                    .body(thumbnail);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
