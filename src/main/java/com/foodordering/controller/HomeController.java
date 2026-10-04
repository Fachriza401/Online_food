package com.foodordering.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.foodordering.model.Food;
import com.foodordering.service.CategoryService;
import com.foodordering.service.FoodService;
import com.foodordering.service.ReviewService;

@Controller
public class HomeController {

    private final FoodService foodService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;

    public HomeController(FoodService foodService, CategoryService categoryService, ReviewService reviewService) {
        this.foodService = foodService;
        this.categoryService = categoryService;
        this.reviewService = reviewService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("activePage", "home");
        Food heroFood = foodService.findAvailable(null, null).stream()
                .filter(f -> f.getName().toLowerCase().contains("nasi goreng")
                        || f.getName().toLowerCase().contains("spesial"))
                .findFirst()
                .orElseGet(() -> foodService.findAvailable(null, null).stream().findFirst().orElse(null));
        List<Food> popular = foodService.findAvailable(null, null).stream().limit(8).toList();
        reviewService.applyRatings(heroFood == null ? List.of() : List.of(heroFood));
        reviewService.applyRatings(popular);
        model.addAttribute("heroFood", heroFood);
        model.addAttribute("categories", categoryService.findAllActive());
        model.addAttribute("popularFoods", popular);
        model.addAttribute("totalMenus", foodService.findAll().size());
        return "index";
    }
}