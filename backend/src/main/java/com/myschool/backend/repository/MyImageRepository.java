package com.myschool.backend.repository;

import com.myschool.backend.models.entity.MyImage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MyImageRepository extends MongoRepository<MyImage, String> {

    @Query("{'user_id': ?0}")
    List<MyImage> findByUserId(String userId);

    @Query("{'id': ?0, 'user_id': ?1}")
    Optional<MyImage> findByIdAndUserId(String id, String userId);
}
