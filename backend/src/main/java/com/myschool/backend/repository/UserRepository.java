package com.myschool.backend.repository;

import com.myschool.backend.models.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    @Query("{'id': ?0}")
    Optional<User> findByIdField(String id);

    Optional<User> findByEmail(String email);

    @Query("{'email': ?0, 'school_code': ?1}")
    Optional<User> findByEmailAndSchoolCode(String email, String schoolCode);

    @Query("{'role': ?0}")
    Optional<User> findFirstByRole(String role);

    @Query("{'school_code': ?0, 'role': ?1}")
    Optional<User> findFirstBySchoolCodeAndRole(String schoolCode, String role);

    @Query("{'school_code': ?0, 'role': ?1}")
    List<User> findBySchoolCodeAndRole(String schoolCode, String role);

    @Query("{'teacher_code': ?0, 'role': 'TEACHER'}")
    Optional<User> findByTeacherCode(String teacherCode);

    @Query("{'mobile_number': ?0}")
    Optional<User> findByMobileNumber(String mobileNumber);

    @Query(value = "{'school_code': ?0, 'role': ?1}", count = true)
    long countBySchoolCodeAndRole(String schoolCode, String role);

    @Query(value = "{'role': ?0}", count = true)
    long countByRole(String role);
}
