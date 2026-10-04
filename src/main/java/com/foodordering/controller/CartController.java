package com.foodordering.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.foodordering.model.Cart;
import com.foodordering.service.CartService;
import com.foodordering.security.CurrentUser;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    @Value("${app.shipping-fee:10000}")
    private long shippingFee;

    @Value("${app.shipping-free-threshold:100000}")
    private long freeShippingThreshold;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public String add(@RequestParam("foodId") Long foodId,
                      @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                      RedirectAttributes ra) {
        cartService.addItem(CurrentUser.get(), foodId, quantity);
        ra.addFlashAttribute("success", "Menu ditambahkan ke keranjang");
        return "redirect:/cart";
    }

    @GetMapping
    public String cart(Model model) {
        Cart cart = cartService.getCart(CurrentUser.get());
        model.addAttribute("cart", cart);
        if (cart != null) {
            BigDecimal subtotal = cart.getSubtotal();
            BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(freeShippingThreshold)) >= 0
                    ? BigDecimal.ZERO : BigDecimal.valueOf(shippingFee);
            model.addAttribute("shippingFee", shipping);
            model.addAttribute("freeShippingThreshold", freeShippingThreshold);
            model.addAttribute("total", subtotal.add(shipping));
        }
        return "cart/cart";
    }

    @PostMapping("/update")
    public String update(@RequestParam("itemId") Long itemId,
                         @RequestParam("quantity") int quantity) {
        cartService.updateQuantity(CurrentUser.get(), itemId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam("itemId") Long itemId) {
        cartService.removeItem(CurrentUser.get(), itemId);
        return "redirect:/cart";
    }
}