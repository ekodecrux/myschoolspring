package com.myschool.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    @Value("${r2.access-key:}")
    private String accessKey;

    @Value("${r2.secret-key:}")
    private String secretKey;

    @Value("${r2.bucket:}")
    private String bucket;

    @Value("${r2.endpoint:}")
    private String endpoint;

    @Value("${r2.public-url:}")
    private String publicUrl;

    @Value("${r2.base-url:}")
    private String baseUrl;

    private S3Client s3Client;

    // In-memory cache for R2 listings (TTL: 5 minutes)
    private final Map<String, Object[]> r2Cache = new HashMap<>();
    private static final long CACHE_TTL_MS = 300_000L;

    @PostConstruct
    public void init() {
        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            try {
                s3Client = S3Client.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)))
                        .endpointOverride(URI.create(endpoint))
                        .region(Region.of("auto"))
                        .forcePathStyle(true)
                        .build();
                logger.info("R2 S3 client initialized successfully");
            } catch (Exception e) {
                logger.warn("Failed to initialize R2 S3 client: {}", e.getMessage());
            }
        } else {
            logger.warn("R2 credentials not configured - storage operations will be unavailable");
        }
    }

    public String getBaseUrl() {
        return (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : publicUrl;
    }

        // List objects in R2 bucket under a given prefix
    public List<Map<String, Object>> listObjects(String prefix) {
        if (s3Client == null) return new ArrayList<>();

        String cacheKey = "list:" + prefix;
        Object[] cached = r2Cache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - (Long) cached[1] < CACHE_TTL_MS) {
            return (List<Map<String, Object>>) cached[0];
        }

        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            List<Map<String, Object>> objects = new ArrayList<>();

            for (S3Object obj : response.contents()) {
                Map<String, Object> item = new HashMap<>();
                item.put("key", obj.key());
                item.put("size", obj.size());
                item.put("url", getBaseUrl() + "/" + obj.key());
                objects.add(item);
            }

            r2Cache.put(cacheKey, new Object[]{objects, System.currentTimeMillis()});
            return objects;
        } catch (Exception e) {
            logger.error("Failed to list R2 objects: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

        // Upload a file to R2
    public String uploadObject(String key, byte[] content, String contentType) {
        if (s3Client == null) throw new RuntimeException("R2 storage not configured");

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(content));
            String base = getBaseUrl(); String k = key.startsWith("/") ? key.substring(1) : key; return base.endsWith("/") ? base + k : base + "/" + k;
        } catch (Exception e) {
            logger.error("Failed to upload to R2: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

        // Delete an object from R2
    public boolean deleteObject(String key) {
        if (s3Client == null) return false;

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete R2 object: {}", e.getMessage());
            return false;
        }
    }

        // Get public URL for a key
    public String getPublicUrl(String key) {
        String base = getBaseUrl(); String k = key.startsWith("/") ? key.substring(1) : key; return base.endsWith("/") ? base + k : base + "/" + k;
    }

        // Clear cache
    public void clearCache() {
        r2Cache.clear();
    }
}
