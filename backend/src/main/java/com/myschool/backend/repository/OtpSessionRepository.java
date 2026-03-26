package com.myschool.backend.repository;

import com.myschool.backend.models.entity.OtpSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpSessionRepository extends MongoRepository<OtpSession, String> {

    @Query("{'phone_number': ?0, 'otp': ?1}")
    Optional<OtpSession> findByPhoneNumberAndOtp(String phoneNumber, String otp);
}
