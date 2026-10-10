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

import com.foodordering.exception.BadRequestException;
import com.foodordering.model.Order;
import com.foodordering.model.OrderStatus;
import com.foodordering.service.OrderService;
import com.foodordering.service.ReportService;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final ReportService reportService;

    public AdminOrderController(OrderService orderService, ReportService reportService) {
        this.orderService = orderService;
        this.reportService = reportService;
    }

    @GetMapping
    public String list(@RequestParam(value = "status", required = false) OrderStatus status,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {
        Page<Order> orders = status == null
                ? orderService.findAllPaged(page, 10)
                : orderService.findByStatusPaged(status, page, 10);
        model.addAttribute("orders", orders);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("statusCounts", reportService.ordersByStatus());
        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Order order = orderService.findById(id);
        model.addAttribute("order", order);
        model.addAttribute("payment", orderService.findPayment(order));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("allowedStatuses", orderService.allowedTransitions(order.getStatus()));
        model.addAttribute("statusFlow", orderService.statusFlow());
        model.addAttribute("statusIndex", orderService.statusIndex(order.getStatus()));
        return "admin/order-detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam("status") OrderStatus status,
                               RedirectAttributes ra) {
        try {
            orderService.updateStatus(id, status);
            ra.addFlashAttribute("success", "Status pesanan diperbarui menjadi " + status.getLabel());
        } catch (BadRequestException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}