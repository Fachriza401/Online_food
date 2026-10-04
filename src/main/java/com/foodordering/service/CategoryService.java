package com.foodordering.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodordering.dto.CategoryForm;
import com.foodordering.exception.BadRequestException;
import com.foodordering.model.Category;
import com.foodordering.repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Category> findAllActive() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Kategori tidak ditemukan"));
    }

    @Transactional
    public Category create(CategoryForm form) {
        if (categoryRepository.existsByName(form.getName())) {
            throw new BadRequestException("Nama kategori sudah digunakan");
        }
        Category category = new Category();
        apply(category, form);
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryForm form) {
        Category category = findById(id);
        apply(category, form);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findById(id);
        if (category.getFoods() != null && !category.getFoods().isEmpty()) {
            throw new BadRequestException("Kategori tidak dapat dihapus karena masih memiliki menu makanan");
        }
        categoryRepository.delete(category);
    }

    private void apply(Category category, CategoryForm form) {
        category.setName(form.getName());
        category.setDescription(form.getDescription());
        category.setImageUrl(form.getImageUrl());
        category.setActive(form.isActive());
    }
}