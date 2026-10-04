package com.foodordering.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.foodordering.model.Food;

public interface FoodRepository extends JpaRepository<Food, Long> {

    List<Food> findAllByOrderByCreatedAtDesc();

    List<Food> findAllByAvailableTrueOrderByNameAsc();

    @Query("SELECT f FROM Food f WHERE f.available = true AND "
            + "(:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            + "AND (:categoryId IS NULL OR f.category.id = :categoryId) "
            + "ORDER BY f.name ASC")
    List<Food> searchAvailable(@Param("keyword") String keyword, @Param("categoryId") Long categoryId);

    @Query("SELECT f FROM Food f WHERE "
            + "(:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            + "AND (:categoryId IS NULL OR f.category.id = :categoryId) "
            + "ORDER BY f.name ASC")
    List<Food> search(@Param("keyword") String keyword, @Param("categoryId") Long categoryId);

    Optional<Food> findByName(String name);
}