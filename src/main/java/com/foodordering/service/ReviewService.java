package com.foodordering.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodordering.exception.BadRequestException;
import com.foodordering.model.Food;
import com.foodordering.model.Review;
import com.foodordering.model.User;
import com.foodordering.repository.FoodRepository;
import com.foodordering.repository.OrderItemRepository;
import com.foodordering.repository.ReviewRepository;

@Service
public class ReviewService {

    private static final int MAX_COMMENT = 500;

    private final ReviewRepository reviewRepository;
    private final FoodRepository foodRepository;
    private final OrderItemRepository orderItemRepository;

    public ReviewService(ReviewRepository reviewRepository, FoodRepository foodRepository,
                         OrderItemRepository orderItemRepository) {
        this.reviewRepository = reviewRepository;
        this.foodRepository = foodRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public void applyRatings(List<Food> foods) {
        if (foods == null || foods.isEmpty()) {
            return;
        }
        Map<Long, double[]> summary = ratingSummary();
        for (Food food : foods) {
            double[] value = summary.get(food.getId());
            if (value == null) {
                food.setRatingAvg(null);
                food.setReviewCount(0);
            } else {
                food.setRatingAvg(Math.round(value[0] * 10.0) / 10.0);
                food.setReviewCount((long) value[1]);
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, double[]> ratingSummary() {
        Map<Long, double[]> summary = new HashMap<>();
        for (Object[] row : reviewRepository.ratingSummary()) {
            summary.put((Long) row[0], new double[] { ((Number) row[1]).doubleValue(), ((Number) row[2]).doubleValue() });
        }
        return summary;
    }

    @Transactional(readOnly = true)
    public List<Review> findByFood(Long foodId) {
        return reviewRepository.findByFoodIdOrderByCreatedAtDesc(foodId);
    }

    @Transactional(readOnly = true)
    public Optional<Review> findMine(Long foodId, Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return reviewRepository.findByFoodIdAndUserId(foodId, userId);
    }

    @Transactional(readOnly = true)
    public boolean canReview(Long userId, Long foodId) {
        if (userId == null) {
            return false;
        }
        if (reviewRepository.findByFoodIdAndUserId(foodId, userId).isPresent()) {
            return true;
        }
        return orderItemRepository.existsCompletedPurchase(userId, foodId);
    }

    @Transactional
    public void submit(Long foodId, User user, int rating, String comment) {
        if (user == null) {
            throw new BadRequestException("Silakan login untuk memberikan ulasan");
        }
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating harus antara 1 sampai 5 bintang");
        }
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new BadRequestException("Menu tidak ditemukan"));
        String raw = comment == null ? "" : comment.trim();
        final String text = raw.length() > MAX_COMMENT ? raw.substring(0, MAX_COMMENT) : raw;
        if (!orderItemRepository.existsCompletedPurchase(user.getId(), foodId)
                && reviewRepository.findByFoodIdAndUserId(foodId, user.getId()).isEmpty()) {
            throw new BadRequestException("Ulasan hanya bisa diberikan untuk menu yang sudah pernah kamu pesan");
        }

        Review review = reviewRepository.findByFoodIdAndUserId(foodId, user.getId())
                .orElseGet(() -> new Review(food, user, rating, text));
        review.setRating(rating);
        review.setComment(text.isEmpty() ? null : text);
        review.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }
}
