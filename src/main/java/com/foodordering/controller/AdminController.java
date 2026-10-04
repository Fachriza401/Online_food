package com.foodordering.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.foodordering.model.OrderStatus;
import com.foodordering.model.PaymentMethod;
import com.foodordering.service.ReportService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ReportService reportService;

    public AdminController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", reportService.dashboardStats());
        model.addAttribute("recentOrders", reportService.recentOrders(8));
        model.addAttribute("ordersByStatus", reportService.ordersByStatus());
        model.addAttribute("salesBar", salesBarData(7));
        return "admin/dashboard";
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("customers", reportService.findAllCustomers());
        return "admin/customers";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("stats", reportService.dashboardStats());
        model.addAttribute("topFoods", reportService.topFoods(10));
        model.addAttribute("ordersByStatusMap", reportService.ordersByStatus());
        model.addAttribute("paymentMethod", PaymentMethod.COD);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("salesPerDay", buildSalesTable(30));
        return "admin/reports";
    }

    private List<Object[]> buildSalesTable(int days) {
        List<Object[]> rows = new ArrayList<>();
        for (Object[] row : reportService.salesPerDay(days)) {
            rows.add(new Object[]{ row[0], row[1] });
        }
        return rows;
    }

    private List<Object[]> salesBarData(int days) {
        Map<LocalDate, BigDecimal> byDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            byDay.put(today.minusDays(i), BigDecimal.ZERO);
        }
        for (Object[] row : reportService.salesPerDay(days)) {
            LocalDate day = ((LocalDateTime) row[0]).toLocalDate();
            byDay.merge(day, (BigDecimal) row[1], BigDecimal::add);
        }
        List<Object[]> rows = new ArrayList<>();
        byDay.forEach((day, total) -> rows.add(new Object[]{ day, total }));
        return rows;
    }
}