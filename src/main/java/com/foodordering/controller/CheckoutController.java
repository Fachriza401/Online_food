package com.foodordering.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.foodordering.dto.CheckoutRequest;
import com.foodordering.exception.BadRequestException;
import com.foodordering.model.Cart;
import com.foodordering.model.Order;
import com.foodordering.security.CurrentUser;
import com.foodordering.service.CartService;
import com.foodordering.service.OrderService;

import jakarta.validation.Valid;

@Controller
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;

    public CheckoutController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @GetMapping("/checkout")
    public String checkout(Model model) {
        Cart cart = cartService.getCart(CurrentUser.get());
        if (cart == null || cart.getItems().isEmpty()) {
            return "redirect:/cart?empty";
        }
        model.addAttribute("cart", cart);
        model.addAttribute("shippingFee", orderService.computeShipping(cart.getSubtotal()));
        model.addAttribute("total", cart.getSubtotal().add(orderService.computeShipping(cart.getSubtotal())));
        CheckoutRequest request = new CheckoutRequest();
        var user = CurrentUser.get();
        request.setCustomerName(user.getFullName());
        request.setCustomerPhone(user.getPhone());
        request.setCustomerAddress(user.getAddress());
        request.setPaymentMethod(com.foodordering.model.PaymentMethod.COD);
        model.addAttribute("checkoutRequest", request);
        addPaymentMethods(model);
        return "checkout/checkout";
    }

    private void addPaymentMethods(Model model) {
        model.addAttribute("codMethods", com.foodordering.model.PaymentMethod.byGroup("cod"));
        model.addAttribute("ewalletMethods", com.foodordering.model.PaymentMethod.byGroup("ewallet"));
        model.addAttribute("bankMethods", com.foodordering.model.PaymentMethod.byGroup("bank"));
    }

    @PostMapping("/checkout")
    public String placeOrder(@Valid @ModelAttribute("checkoutRequest") CheckoutRequest request,
                             BindingResult bindingResult, Model model, RedirectAttributes ra) {
        Cart cart = cartService.getCart(CurrentUser.get());
        if (cart == null || cart.getItems().isEmpty()) {
            return "redirect:/cart?empty";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("cart", cart);
            model.addAttribute("shippingFee", orderService.computeShipping(cart.getSubtotal()));
            model.addAttribute("total", cart.getSubtotal().add(orderService.computeShipping(cart.getSubtotal())));
            addPaymentMethods(model);
            return "checkout/checkout";
        }
        try {
            Order order = orderService.placeOrder(CurrentUser.get(), request);
            ra.addFlashAttribute("success", "Pesanan berhasil dibuat. Kode: " + order.getOrderCode());
            return "redirect:/order/success/" + order.getId();
        } catch (BadRequestException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("cart", cart);
            model.addAttribute("shippingFee", orderService.computeShipping(cart.getSubtotal()));
            model.addAttribute("total", cart.getSubtotal().add(orderService.computeShipping(cart.getSubtotal())));
            addPaymentMethods(model);
            return "checkout/checkout";
        }
    }

    @GetMapping("/order/success/{id}")
    public String success(@PathVariable("id") Long id, Model model) {
        Order order = orderService.findOwnedOrder(CurrentUser.get(), id);
        model.addAttribute("order", order);
        model.addAttribute("payment", orderService.findPayment(order));
        return "order/success";
    }
}