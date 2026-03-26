package com.myschool.backend.service;

import com.myschool.backend.exception.AppException;
import com.myschool.backend.models.entity.MakerTemplate;
import com.myschool.backend.models.entity.User;
import com.myschool.backend.models.entity.UserRole;
import com.myschool.backend.repository.MakerTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TemplateService {

    @Autowired private MakerTemplateRepository templateRepository;
    @Autowired private MongoTemplate mongoTemplate;

    public Map<String, Object> listTemplates(String makerType, User currentUser) {
        // Get user templates + system templates
        Query query = new Query();
        List<Criteria> orConditions = new ArrayList<>();
        orConditions.add(Criteria.where("user_id").is(currentUser.getId()));
        orConditions.add(Criteria.where("isSystem").is(true));

        query.addCriteria(new Criteria().orOperator(orConditions.toArray(new Criteria[0])));

        if (makerType != null && !makerType.isEmpty()) {
            query.addCriteria(Criteria.where("makerType").is(makerType));
        }

        List<MakerTemplate> templates = mongoTemplate.find(query, MakerTemplate.class);

        return Map.of(
                "data", templates.stream().map(this::buildTemplateResponse).collect(Collectors.toList()),
                "total", templates.size()
        );
    }

    public Map<String, Object> saveTemplate(Map<String, Object> body, User currentUser) {
        String id = (String) body.get("id");
        String name = (String) body.get("name");
        String makerType = (String) body.get("makerType");
        String pageSize = (String) body.get("pageSize");
        String canvasBg = (String) body.get("canvasBg");
        List<Map<String, Object>> elements = (List<Map<String, Object>>) body.get("elements");

        if (id != null && !id.isEmpty()) {
            // Update existing
            Optional<MakerTemplate> existing = templateRepository.findByIdAndUserId(id, currentUser.getId());
            if (existing.isPresent()) {
                MakerTemplate template = existing.get();
                if (name != null) template.setName(name);
                if (pageSize != null) template.setPageSize(pageSize);
                if (canvasBg != null) template.setCanvasBg(canvasBg);
                if (elements != null) template.setElements(elements);
                template.setUpdatedAt(Instant.now().toString());
                templateRepository.save(template);
                return Map.of("message", "Template updated", "id", template.getId());
            }
        }

        // Create new
        String templateId = UUID.randomUUID().toString();
        MakerTemplate template = MakerTemplate.builder()
                .id(templateId)
                .userId(currentUser.getId())
                .name(name != null ? name : "Untitled Template")
                .makerType(makerType)
                .pageSize(pageSize)
                .canvasBg(canvasBg)
                .elements(elements)
                .isSystem(false)
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .build();

        templateRepository.save(template);
        return Map.of("message", "Template saved", "id", templateId);
    }

    public Map<String, Object> deleteTemplate(String templateId, User currentUser) {
        MakerTemplate template = templateRepository.findByIdAndUserId(templateId, currentUser.getId())
                .orElseThrow(() -> new AppException("Template not found", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(template.getIsSystem()) && !UserRole.SUPER_ADMIN.equals(currentUser.getRole())) {
            throw new AppException("Cannot delete system templates", HttpStatus.FORBIDDEN);
        }

        templateRepository.delete(template);
        return Map.of("message", "Template deleted");
    }

    private Map<String, Object> buildTemplateResponse(MakerTemplate t) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("name", t.getName());
        map.put("makerType", t.getMakerType());
        map.put("pageSize", t.getPageSize());
        map.put("canvasBg", t.getCanvasBg());
        map.put("elements", t.getElements());
        map.put("isSystem", t.getIsSystem());
        map.put("createdAt", t.getCreatedAt());
        map.put("updatedAt", t.getUpdatedAt());
        return map;
    }
}
