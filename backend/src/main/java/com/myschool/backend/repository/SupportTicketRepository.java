package com.myschool.backend.repository;

import com.myschool.backend.models.entity.SupportTicket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends MongoRepository<SupportTicket, String> {

    @Query("{'user_id': ?0}")
    List<SupportTicket> findByUserId(String userId);

    @Query("{'id': ?0}")
    Optional<SupportTicket> findByTicketId(String id);
}
