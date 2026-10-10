package com.foodordering.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.foodordering.model.Order;
import com.foodordering.security.CurrentUser;
import com.foodordering.service.OrderService;

@Controller
@RequestMapping("/orders")
public class CustomerOrderController {

    private final OrderService orderService;

    public CustomerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String list(@RequestParam(value = "tab", defaultValue = "active") String tab,
                       @RequestParam(value = "page", defaultValue = "0") int page, Model model) {
        model.addAttribute("activePage", "orders");
        boolean history = "history".equals(tab);
        Page<Order> orders = orderService.findByUserAndStatuses(CurrentUser.get(),
                history ? com.foodordering.service.OrderService.HISTORY_STATUSES
                        : com.foodordering.service.OrderService.ACTIVE_STATUSES,
                page, 10);
        model.addAttribute("orders", orders);
        model.addAttribute("tab", history ? "history" : "active");
        return "order/list";
    }

    @PostMapping("/{id}/pay")
    public String pay(@PathVariable("id") Long id, RedirectAttributes ra) {
        orderService.markPaid(CurrentUser.get(), id);
        ra.addFlashAttribute("success", "Pembayaran berhasil dikonfirmasi");
        return "redirect:/orders/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("activePage", "orders");
        Order order = orderService.findOwnedOrder(CurrentUser.get(), id);
        model.addAttribute("order", order);
        model.addAttribute("payment", orderService.findPayment(order));
        model.addAttribute("statusFlow", orderService.statusFlow());
        model.addAttribute("statusIndex", orderService.statusIndex(order.getStatus()));
        return "order/detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable("id") Long id, RedirectAttributes ra) {
        orderService.cancelByCustomer(CurrentUser.get(), id);
        ra.addFlashAttribute("success", "Pesanan dibatalkan");
        return "redirect:/orders/" + id;
    }
}