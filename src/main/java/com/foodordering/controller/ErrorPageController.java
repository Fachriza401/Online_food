package com.foodordering.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Halaman error statis. {@code SecurityConfig} meneruskan {@code AccessDeniedException} ke
 * {@code /403}, jadi endpoint ini wajib ada dan harus boleh diakses tanpa login.
 */
@Controller
public class ErrorPageController {

    @GetMapping("/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden(Model model) {
        model.addAttribute("status", HttpStatus.FORBIDDEN.value());
        return "error/403";
    }

    @GetMapping("/404")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(Model model) {
        model.addAttribute("status", HttpStatus.NOT_FOUND.value());
        return "error/404";
    }
}