package com.foodordering.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.foodordering.exception.BadRequestException;
import com.foodordering.model.User;
import com.foodordering.security.CurrentUser;
import com.foodordering.service.FileStorageService;
import com.foodordering.service.ProfileService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final FileStorageService fileStorageService;

    public ProfileController(ProfileService profileService, FileStorageService fileStorageService) {
        this.profileService = profileService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String profile(Model model) {
        model.addAttribute("user", CurrentUser.get());
        return "profile/profile";
    }

    @PostMapping
    public String update(@Valid @ModelAttribute("user") User form,
                         @RequestParam(name = "photo", required = false) MultipartFile photo,
                         RedirectAttributes ra) {
        String newAvatarUrl = null;
        try {
            if (photo != null && !photo.isEmpty()) {
                newAvatarUrl = fileStorageService.storeAvatar(photo, CurrentUser.get().getId());
            }
            profileService.update(CurrentUser.get(), form, newAvatarUrl);
            ra.addFlashAttribute("success", "Profil berhasil diperbarui");
        } catch (BadRequestException e) {
            if (newAvatarUrl != null) {
                fileStorageService.deleteIfUploaded(newAvatarUrl);
            }
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@org.springframework.web.bind.annotation.RequestParam("oldPassword") String oldPassword,
                                 @org.springframework.web.bind.annotation.RequestParam("newPassword") String newPassword,
                                 RedirectAttributes ra) {
        try {
            profileService.changePassword(CurrentUser.get(), oldPassword, newPassword);
            ra.addFlashAttribute("success", "Kata sandi berhasil diperbarui");
        } catch (BadRequestException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }
}