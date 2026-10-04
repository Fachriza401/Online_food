package com.foodordering.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.foodordering.exception.BadRequestException;
import com.foodordering.model.User;
import com.foodordering.security.CurrentUser;
import com.foodordering.service.ReviewService;

@Controller
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/food/{id}/review")
    public String submit(@PathVariable Long id,
                         @RequestParam(value = "rating", defaultValue = "0") int rating,
                         @RequestParam(value = "comment", required = false) String comment,
                         RedirectAttributes ra) {
        User user = CurrentUser.get();
        if (user == null) {
            return "redirect:/login";
        }
        try {
            reviewService.submit(id, user, rating, comment);
            ra.addFlashAttribute("success", "Terima kasih, ulasanmu sudah tersimpan");
        } catch (BadRequestException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/food/" + id;
    }
}
