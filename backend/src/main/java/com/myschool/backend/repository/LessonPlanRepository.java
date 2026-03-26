package com.myschool.backend.repository;

import com.myschool.backend.models.entity.LessonPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonPlanRepository extends MongoRepository<LessonPlan, String> {

    @Query("{'user_id': ?0}")
    List<LessonPlan> findByUserId(String userId);

    @Query("{'id': ?0, 'user_id': ?1}")
    Optional<LessonPlan> findByIdAndUserId(String id, String userId);
}
