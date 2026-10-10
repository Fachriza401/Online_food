package com.foodordering.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodordering.dto.FoodForm;
import com.foodordering.exception.BadRequestException;
import com.foodordering.model.Category;
import com.foodordering.model.Food;
import com.foodordering.repository.CartItemRepository;
import com.foodordering.repository.CategoryRepository;
import com.foodordering.repository.FoodRepository;
import com.foodordering.repository.OrderItemRepository;
import com.foodordering.repository.ReviewRepository;

@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final CategoryRepository categoryRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;

    public FoodService(FoodRepository foodRepository, CategoryRepository categoryRepository,
                       CartItemRepository cartItemRepository, OrderItemRepository orderItemRepository,
                       ReviewRepository reviewRepository) {
        this.foodRepository = foodRepository;
        this.categoryRepository = categoryRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public List<Food> findAll() {
        return foodRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Food> findAvailable(String keyword, Long categoryId) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return foodRepository.searchAvailable(kw, categoryId);
    }

    @Transactional(readOnly = true)
    public List<Food> findAdmin(String keyword, Long categoryId) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return foodRepository.search(kw, categoryId);
    }

    @Transactional(readOnly = true)
    public List<Food> findPopular(int limit) {
        List<Food> available = foodRepository.findAllByAvailableTrueOrderByNameAsc();
        return available.stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public Page<Food> findAllPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return foodRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Food findById(Long id) {
        return foodRepository.findById(id).orElseThrow(() -> new BadRequestException("Menu tidak ditemukan"));
    }

    @Transactional(readOnly = true)
    public Food findAvailableById(Long id) {
        Food food = findById(id);
        if (!food.isAvailable()) {
            throw new BadRequestException("Menu tidak tersedia");
        }
        return food;
    }

    @Transactional
    public Food create(FoodForm form) {
        Food food = new Food();
        apply(food, form);
        return foodRepository.save(food);
    }

    @Transactional
    public Food update(Long id, FoodForm form) {
        Food food = findById(id);
        apply(food, form);
        return foodRepository.save(food);
    }

    @Transactional
    public void delete(Long id) {
        Food food = findById(id);
        if (orderItemRepository.existsByFoodId(id) || reviewRepository.countByFoodId(id) > 0) {
            throw new BadRequestException("Menu \"" + food.getName() + "\" tidak dapat dihapus karena sudah "
                    + "miliki pesanan atau ulasan. Ubah menu menjadi tidak tersedia saja.");
        }
        cartItemRepository.deleteByFoodId(id);
        foodRepository.delete(food);
    }

    private void apply(Food food, FoodForm form) {
        food.setName(form.getName());
        food.setDescription(form.getDescription());
        food.setPrice(form.getPrice());
        food.setAvailable(form.isAvailable());
        if (form.getImageUrl() != null && !form.getImageUrl().isBlank()) {
            food.setImageUrl(form.getImageUrl());
        }
        if (form.getCategoryId() != null) {
            Category category = categoryRepository.findById(form.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Kategori tidak ditemukan"));
            food.setCategory(category);
        } else {
            food.setCategory(null);
        }
    }
}