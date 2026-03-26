package com.myschool.backend.repository;

import com.myschool.backend.models.entity.MakerTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MakerTemplateRepository extends MongoRepository<MakerTemplate, String> {

    @Query("{'user_id': ?0, 'makerType': ?1}")
    List<MakerTemplate> findByUserIdAndMakerType(String userId, String makerType);

    @Query("{'id': ?0, 'user_id': ?1}")
    Optional<MakerTemplate> findByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);
}
