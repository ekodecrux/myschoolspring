package com.myschool.backend.repository;

import com.myschool.backend.models.entity.PasswordReset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetRepository extends MongoRepository<PasswordReset, String> {

    Optional<PasswordReset> findByEmailAndCode(String email, String code);

    void deleteByEmail(String email);
}
