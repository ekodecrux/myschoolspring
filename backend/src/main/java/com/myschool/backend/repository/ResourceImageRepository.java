package com.myschool.backend.repository;

import com.myschool.backend.models.entity.ResourceImage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceImageRepository extends MongoRepository<ResourceImage, String> {

    @Query("{'id': ?0}")
    Optional<ResourceImage> findByImageId(String id);

    @Query(value = "{'category': ?0}", count = true)
    long countByCategory(String category);

    @Query(value = "{'category': ?0, 'subcategory': ?1}", count = true)
    long countByCategoryAndSubcategory(String category, String subcategory);
}
