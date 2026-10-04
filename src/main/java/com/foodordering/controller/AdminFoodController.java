package com.foodordering.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.foodordering.dto.FoodForm;
import com.foodordering.exception.BadRequestException;
import com.foodordering.service.CategoryService;
import com.foodordering.service.FileStorageService;
import com.foodordering.service.FoodService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/foods")
public class AdminFoodController {

    private final FoodService foodService;
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

    public AdminFoodController(FoodService foodService, CategoryService categoryService,
                               FileStorageService fileStorageService) {
        this.foodService = foodService;
        this.categoryService = categoryService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String keyword,
                       @RequestParam(value = "category", required = false) Long categoryId,
                       Model model) {
        model.addAttribute("foods", foodService.findAdmin(keyword, categoryId));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", categoryId);
        return "admin/foods";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("foodForm", new FoodForm());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("mode", "create");
        model.addAttribute("formAction", "/admin/foods/new");
        return "admin/food-form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("foodForm") FoodForm form,
                         BindingResult bindingResult,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("mode", "create");
            model.addAttribute("formAction", "/admin/foods/new");
            return "admin/food-form";
        }
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                form.setImageUrl(fileStorageService.store(imageFile));
            }
            foodService.create(form);
            ra.addFlashAttribute("success", "Menu berhasil ditambahkan");
            return "redirect:/admin/foods";
        } catch (BadRequestException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("mode", "create");
            model.addAttribute("formAction", "/admin/foods/new");
            return "admin/food-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var food = foodService.findById(id);
        FoodForm form = new FoodForm();
        form.setName(food.getName());
        form.setDescription(food.getDescription());
        form.setPrice(food.getPrice());
        form.setCategoryId(food.getCategory() != null ? food.getCategory().getId() : null);
        form.setAvailable(food.isAvailable());
        form.setImageUrl(food.getImageUrl());
        model.addAttribute("foodForm", form);
        model.addAttribute("food", food);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("mode", "edit");
        model.addAttribute("formAction", "/admin/foods/" + id + "/edit");
        return "admin/food-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("foodForm") FoodForm form,
                         BindingResult bindingResult,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("food", foodService.findById(id));
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("mode", "edit");
            model.addAttribute("formAction", "/admin/foods/" + id + "/edit");
            return "admin/food-form";
        }
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                form.setImageUrl(fileStorageService.store(imageFile));
            }
            foodService.update(id, form);
            ra.addFlashAttribute("success", "Menu berhasil diperbarui");
            return "redirect:/admin/foods";
        } catch (BadRequestException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("food", foodService.findById(id));
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("mode", "edit");
            model.addAttribute("formAction", "/admin/foods/" + id + "/edit");
            return "admin/food-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        foodService.delete(id);
        ra.addFlashAttribute("success", "Menu dihapus");
        return "redirect:/admin/foods";
    }
}