package com.myschool.backend.repository;

import com.myschool.backend.models.entity.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    @Query("{'id': ?0}")
    Optional<Order> findByOrderId(String id);

    @Query("{'user_id': ?0}")
    List<Order> findByUserId(String userId);

    @Query("{'razorpay_order_id': ?0}")
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    @Query("{'stripe_session_id': ?0}")
    Optional<Order> findByStripeSessionId(String sessionId);
}
