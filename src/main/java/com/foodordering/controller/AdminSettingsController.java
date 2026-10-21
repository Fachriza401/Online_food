package com.foodordering.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.foodordering.exception.BadRequestException;
import com.foodordering.model.RestaurantSetting;
import com.foodordering.service.FileStorageService;
import com.foodordering.service.RestaurantSettingService;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final RestaurantSettingService settingService;
    private final FileStorageService fileStorageService;

    public AdminSettingsController(RestaurantSettingService settingService,
                                   FileStorageService fileStorageService) {
        this.settingService = settingService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String editForm(Model model) {
        model.addAttribute("setting", settingService.get());
        return "admin/settings";
    }

    @PostMapping
    public String update(@RequestParam(value = "restaurantName", required = false) String restaurantName,
                         @RequestParam(value = "address", required = false) String address,
                         @RequestParam(value = "phone", required = false) String phone,
                         @RequestParam(value = "email", required = false) String email,
                         @RequestParam(value = "description", required = false) String description,
                         @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                         Model model, RedirectAttributes ra) {
        RestaurantSetting incoming = new RestaurantSetting();
        incoming.setRestaurantName(restaurantName);
        incoming.setAddress(address);
        incoming.setPhone(phone);
        incoming.setEmail(email);
        incoming.setDescription(description);
        try {
            String newLogoUrl = null;
            if (logoFile != null && !logoFile.isEmpty()) {
                newLogoUrl = fileStorageService.store(logoFile, "logo");
            }
            settingService.update(incoming, newLogoUrl);
            ra.addFlashAttribute("success", "Pengaturan restoran berhasil diperbarui");
            return "redirect:/admin/settings";
        } catch (BadRequestException e) {
            model.addAttribute("setting", settingService.get());
            model.addAttribute("error", e.getMessage());
            return "admin/settings";
        }
    }
}
