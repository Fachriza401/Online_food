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

import com.foodordering.dto.CategoryForm;
import com.foodordering.exception.BadRequestException;
import com.foodordering.service.CategoryService;
import com.foodordering.service.FileStorageService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

    public AdminCategoryController(CategoryService categoryService, FileStorageService fileStorageService) {
        this.categoryService = categoryService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        model.addAttribute("mode", "create");
        model.addAttribute("formAction", "/admin/categories/new");
        return "admin/category-form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult bindingResult,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            model.addAttribute("formAction", "/admin/categories/new");
            return "admin/category-form";
        }
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                form.setImageUrl(fileStorageService.store(imageFile));
            }
            categoryService.create(form);
            ra.addFlashAttribute("success", "Kategori berhasil ditambahkan");
            return "redirect:/admin/categories";
        } catch (BadRequestException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("mode", "create");
            model.addAttribute("formAction", "/admin/categories/new");
            return "admin/category-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var category = categoryService.findById(id);
        CategoryForm form = new CategoryForm();
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        form.setImageUrl(category.getImageUrl());
        form.setActive(category.isActive());
        model.addAttribute("categoryForm", form);
        model.addAttribute("category", category);
        model.addAttribute("mode", "edit");
        model.addAttribute("formAction", "/admin/categories/" + id + "/edit");
        return "admin/category-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult bindingResult,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("category", categoryService.findById(id));
            model.addAttribute("mode", "edit");
            model.addAttribute("formAction", "/admin/categories/" + id + "/edit");
            return "admin/category-form";
        }
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                form.setImageUrl(fileStorageService.store(imageFile));
            }
            categoryService.update(id, form);
            ra.addFlashAttribute("success", "Kategori berhasil diperbarui");
            return "redirect:/admin/categories";
        } catch (BadRequestException e) {
            model.addAttribute("category", categoryService.findById(id));
            model.addAttribute("mode", "edit");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("formAction", "/admin/categories/" + id + "/edit");
            return "admin/category-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoryService.delete(id);
            ra.addFlashAttribute("success", "Kategori dihapus");
        } catch (BadRequestException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
