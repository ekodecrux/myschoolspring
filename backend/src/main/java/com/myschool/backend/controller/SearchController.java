package com.myschool.backend.controller;

import com.myschool.backend.models.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.bson.Document;

import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/rest/search")
public class SearchController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping("/global")
    public ResponseEntity<Map<String, Object>> globalSearch(
            @RequestParam String query,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User currentUser) {

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("results", List.of(), "total", 0));
        }

        String searchTerm = query.trim();
        Pattern pattern = Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE);

        List<Map<String, Object>> results = new ArrayList<>();

        // Search R2 images by key path
        try {
            List<Document> images = mongoTemplate.getDb()
                .getCollection("resource_images")
                .find(new Document("$or", List.of(
                    new Document("key", new Document("$regex", searchTerm).append("$options", "i")),
                    new Document("meta_name", new Document("$regex", searchTerm).append("$options", "i")),
                    new Document("tags", new Document("$regex", searchTerm).append("$options", "i"))
                )))
                .limit(size)
                .into(new ArrayList<>());

            for (Document img : images) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", img.getString("id"));
                item.put("type", "image");
                item.put("key", img.getString("key"));
                item.put("url", img.getString("url"));
                item.put("metaName", img.getString("meta_name"));
                item.put("category", img.getString("category"));
                results.add(item);
            }
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Map.of("results", results, "total", results.size(), "query", searchTerm));
    }
}
