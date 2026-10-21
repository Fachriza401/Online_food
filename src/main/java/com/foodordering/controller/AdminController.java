package com.foodordering.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.foodordering.service.ReportExcelService;
import com.foodordering.service.ReportService;
import com.foodordering.service.ReportViewService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ReportService reportService;
    private final ReportExcelService reportExcelService;
    private final ReportViewService reportViewService;

    public AdminController(ReportService reportService, ReportExcelService reportExcelService,
                           ReportViewService reportViewService) {
        this.reportService = reportService;
        this.reportExcelService = reportExcelService;
        this.reportViewService = reportViewService;
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

    /** Halaman laporan di layar + pratinjau cetak (modal). */
    @GetMapping("/reports")
    public String reports(Model model) {
        reportViewService.populate(model, 10);
        return "admin/reports";
    }

    /**
     * Halaman cetak mandiri (responsif, siap "Save as PDF").
     * Dibuka di tab baru sehingga tidak mengganggu halaman laporan.
     */
    @GetMapping("/reports/print")
    public String printReport(Model model) {
        reportViewService.populate(model, 10);
        return "admin/report-print";
    }

    /** Unduh laporan penjualan dalam format Excel (.xlsx). */
    @GetMapping("/reports/excel")
    public ResponseEntity<byte[]> downloadLaporanExcel() throws IOException {
        byte[] content = reportExcelService.buildLaporanPenjualan();
        String filename = "Laporan-Penjualan-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content);
    }

    private List<Object[]> salesBarData(int days) {
        Map<LocalDate, BigDecimal> byDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            byDay.put(today.minusDays(i), BigDecimal.ZERO);
        }
        for (Object[] row : reportService.salesPerDay(days)) {
            LocalDate day = (LocalDate) row[0];
            byDay.merge(day, (BigDecimal) row[1], BigDecimal::add);
        }
        List<Object[]> rows = new ArrayList<>();
        byDay.forEach((day, total) -> rows.add(new Object[]{ day, total }));
        return rows;
    }
}
