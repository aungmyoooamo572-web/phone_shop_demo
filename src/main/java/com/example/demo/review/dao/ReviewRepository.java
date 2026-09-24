package com.example.demo.review.dao;

import com.example.demo.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByPhoneId(UUID phoneId);

    List<Review> findByUserId(UUID userId);

    boolean existsByUserIdAndPhoneId(UUID userId, UUID phoneId);

}
