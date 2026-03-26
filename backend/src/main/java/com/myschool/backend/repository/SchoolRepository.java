package com.myschool.backend.repository;

import com.myschool.backend.models.entity.School;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolRepository extends MongoRepository<School, String> {

    Optional<School> findByCode(String code);

    @Query("{'code': ?0}")
    Optional<School> findBySchoolCode(String code);

    @Query("{'is_active': true}")
    List<School> findAllActiveSchools();

    boolean existsByCode(String code);
}
