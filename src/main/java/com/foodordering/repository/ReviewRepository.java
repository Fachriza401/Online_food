package com.foodordering.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.foodordering.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByFoodIdOrderByCreatedAtDesc(Long foodId);

    Optional<Review> findByFoodIdAndUserId(Long foodId, Long userId);

    long countByFoodId(Long foodId);

    @Query("SELECT r.food.id, AVG(r.rating), COUNT(r) FROM Review r GROUP BY r.food.id")
    List<Object[]> ratingSummary();

    @Query("SELECT r.food.id FROM Review r WHERE r.user.id = :userId")
    List<Long> findReviewedFoodIds(@Param("userId") Long userId);
}
