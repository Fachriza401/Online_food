package com.foodordering.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.foodordering.exception.BadRequestException;
import com.foodordering.model.User;
import com.foodordering.security.CurrentUser;
import com.foodordering.service.CategoryService;
import com.foodordering.service.FoodService;
import com.foodordering.service.ReviewService;

@Controller
public class MenuController {

    private final FoodService foodService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;

    public MenuController(FoodService foodService, CategoryService categoryService, ReviewService reviewService) {
        this.foodService = foodService;
        this.categoryService = categoryService;
        this.reviewService = reviewService;
    }

    @GetMapping("/menu")
    public String menu(@RequestParam(value = "q", required = false) String keyword,
                       @RequestParam(value = "category", required = false) Long categoryId,
                       Model model) {
        model.addAttribute("activePage", "menu");
        var foods = foodService.findAvailable(keyword, categoryId);
        reviewService.applyRatings(foods);
        model.addAttribute("foods", foods);
        model.addAttribute("categories", categoryService.findAllActive());
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("keyword", keyword);
        return "menu/menu";
    }

    @GetMapping("/food/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("activePage", "menu");
        try {
            var food = foodService.findAvailableById(id);
            User user = CurrentUser.get();
            reviewService.applyRatings(List.of(food));
            model.addAttribute("food", food);
            model.addAttribute("reviews", reviewService.findByFood(id));
            model.addAttribute("myReview", reviewService.findMine(id, user == null ? null : user.getId())
                    .orElse(null));
            model.addAttribute("canReview", reviewService.canReview(user == null ? null : user.getId(), id));
        } catch (BadRequestException e) {
            return "redirect:/menu?notFound";
        }
        return "menu/detail";
    }
}