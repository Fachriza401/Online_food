package com.foodordering.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.foodordering.exception.BadRequestException;
import com.foodordering.util.BackUrl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    public String handleBadRequest(BadRequestException ex, RedirectAttributes redirectAttributes,
                                   HttpServletRequest request) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + BackUrl.path(request, "/");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNotFound(NoResourceFoundException ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        return renderError(model, ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, Model model, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, HttpServletResponse response) {
        // Respons sudah ter-commit (mis. stream SSE yang menulis langsung ke output stream):
        // merender view akan memicu IllegalStateException "getOutputStream() has already been
        // called". Tandai request sudah ditangani supaya DispatcherServlet tidak render apa pun.
        if (response.isCommitted()) {
            log.warn("Permintaan {} gagal dengan {} setelah respons ter-commit, operasi dibatalkan",
                    webRequest.getDescription(false), ex.getClass().getSimpleName());
            mavContainer.setRequestHandled(true);
            return null;
        }

        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return renderError(model, ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String renderError(Model model, Exception ex, HttpStatus status) {
        log.error("Kesalahan pada halaman {}: {}", status.value(), ex.getMessage(), ex);
        model.addAttribute("status", status.value());
        model.addAttribute("message", ex.getMessage());
        return "error/" + status.value();
    }
}