package com.foodordering.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.foodordering.model.User;
import com.foodordering.security.UserPrincipal;
import com.foodordering.service.CartService;
import com.foodordering.service.NotificationService;

@ControllerAdvice
public class GlobalModelAdvice {

    private final CartService cartService;
    private final NotificationService notificationService;

    public GlobalModelAdvice(CartService cartService, NotificationService notificationService) {
        this.cartService = cartService;
        this.notificationService = notificationService;
    }

    @ModelAttribute
    public void addGlobals(Model model, HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/")) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String);
        model.addAttribute("isAuthenticated", authenticated);
        model.addAttribute("isAdmin", authenticated && hasRole(auth, "ROLE_ADMIN"));
        model.addAttribute("isCustomer", authenticated && hasRole(auth, "ROLE_CUSTOMER"));
        model.addAttribute("currentUsername",
                authenticated ? auth.getName() : "");
        if (authenticated && auth.getPrincipal() instanceof UserPrincipal principal) {
            User user = principal.getUser();
            model.addAttribute("currentUser", user);
            model.addAttribute("cartCount", cartService.getCartItemCount(user));
            model.addAttribute("cartTotal", cartService.getCartSubtotal(user));
            model.addAttribute("notifUnread", notificationService.unreadCount(user));
            model.addAttribute("notifList", notificationService.recent(user));
        } else {
            model.addAttribute("cartCount", 0);
            model.addAttribute("cartTotal", java.math.BigDecimal.ZERO);
            model.addAttribute("notifUnread", 0);
            model.addAttribute("notifList", java.util.List.of());
        }
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}