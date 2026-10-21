package com.foodordering.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.foodordering.model.OrderStatus;
import com.foodordering.model.PaymentMethod;

/**
 * Menyusun data tampilan (view-model) untuk halaman laporan admin:
 * pratinjau cetak di modal maupun halaman cetak mandiri.
 *
 * <p>Tujuannya agar template Thymeleaf tetap sederhana dan bebas perhitung
 * format angka/tanggal yang rawan error: semua nilai sudah diformat lebih
 * dulu di sini (angka ribuan gaya Indonesia, tanggal berbahasa Indonesia,
 * persentase, serta lebar bar proporsi).</p>
 */
@Service
public class ReportViewService {

    /** Periode default laporan: 30 hari terakhir. */
    public static final int DEFAULT_PERIOD_DAYS = 30;

    private static final Locale LOCALE_ID = new Locale("id", "ID");

    private static final DateTimeFormatter DATE_LONG_ID =
            DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", LOCALE_ID);
    private static final DateTimeFormatter DATE_SHORT_ID =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", LOCALE_ID);
    private static final DateTimeFormatter STAMP_ID =
            DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", LOCALE_ID);
    private static final DateTimeFormatter DOC_NO_ID =
            DateTimeFormatter.ofPattern("yyyyMMdd", LOCALE_ID);

    private final ReportService reportService;

    public ReportViewService(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Mengisi model halaman laporan dengan data siap tampil.
     *
     * @param model     model halaman
     * @param topLimit  jumlah menu terlaris yang ditampilkan
     */
    public void populate(Model model, int topLimit) {
        Map<String, Object> stats = reportService.dashboardStats();
        List<Object[]> topFoods = reportService.topFoods(topLimit);
        Map<OrderStatus, Long> ordersByStatusMap = reportService.ordersByStatus();
        List<Object[]> salesPerDay = reportService.salesPerDay(DEFAULT_PERIOD_DAYS);

        LocalDate periodEnd = LocalDate.now();
        LocalDate periodStart = periodEnd.minusDays(DEFAULT_PERIOD_DAYS - 1);
        LocalDateTime now = LocalDateTime.now();

        long totalItemSold = sumLongColumn(topFoods, 1);
        BigDecimal totalTopRevenue = sumDecimalColumn(topFoods, 2);
        BigDecimal totalSales = sumDecimalColumn(salesPerDay, 1);
        long totalStatusCount = ordersByStatusMap.values().stream()
                .mapToLong(Long::longValue).sum();

        // ---- Model dasar (dipakai halaman laporan di layar) ----
        model.addAttribute("stats", stats);
        model.addAttribute("topFoods", topFoods);
        model.addAttribute("ordersByStatusMap", ordersByStatusMap);
        model.addAttribute("paymentMethod", PaymentMethod.COD);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("salesPerDay", salesPerDay);
        model.addAttribute("totalItemSold", totalItemSold);
        model.addAttribute("totalTopRevenue", totalTopRevenue);
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("totalStatusCount", totalStatusCount);
        model.addAttribute("periodStart", periodStart);
        model.addAttribute("periodEnd", periodEnd);

        // ---- Model siap tampil untuk lembar laporan (cetak/PDF) ----
        model.addAttribute("reportTopFoods", buildTopFoodRows(topFoods));
        model.addAttribute("reportStatuses", buildStatusRows(ordersByStatusMap, totalStatusCount));
        model.addAttribute("reportDaily", buildDailyRows(salesPerDay));
        model.addAttribute("totalItemSoldText", formatNumber(totalItemSold));
        model.addAttribute("totalTopRevenueText", formatRupiah(totalTopRevenue));
        model.addAttribute("totalSalesText", formatRupiah(totalSales));
        model.addAttribute("totalStatusCountText", formatNumber(totalStatusCount));
        model.addAttribute("periodStartText", DATE_SHORT_ID.format(periodStart));
        model.addAttribute("periodEndText", DATE_SHORT_ID.format(periodEnd));
        model.addAttribute("periodLabelText", DATE_SHORT_ID.format(periodStart)
                + " – " + DATE_SHORT_ID.format(periodEnd));
        model.addAttribute("docNumberText", "LPR/" + DOC_NO_ID.format(periodEnd));
        model.addAttribute("printTimestampText", STAMP_ID.format(now));
        model.addAttribute("printDateSignText", DATE_SHORT_ID.format(now));
    }

    /* ============================================================
       PEMBENTUK BARIS TABEL LAPORAN
       ============================================================ */

    private List<Map<String, Object>> buildTopFoodRows(List<Object[]> topFoods) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] data : topFoods) {
            long sold = asLong(data[1]);
            BigDecimal revenue = asBigDecimal(data[2]);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", rank++);
            row.put("name", String.valueOf(data[0]));
            row.put("soldText", formatNumber(sold));
            row.put("revenueText", formatRupiah(revenue));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> buildStatusRows(Map<OrderStatus, Long> statusMap,
                                                       long total) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<OrderStatus, Long> entry : statusMap.entrySet()) {
            long count = entry.getValue();
            double pct = total > 0 ? count * 100.0 / total : 0d;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", entry.getKey().getLabel());
            row.put("badgeClass", entry.getKey().getBadgeClass());
            row.put("countText", formatNumber(count));
            row.put("pct", round2(pct));
            row.put("pctText", formatPercent(pct));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> buildDailyRows(List<Object[]> salesPerDay) {
        BigDecimal max = BigDecimal.ZERO;
        for (Object[] data : salesPerDay) {
            BigDecimal value = asBigDecimal(data[1]);
            if (value.compareTo(max) > 0) {
                max = value;
            }
        }
        double maxDouble = max.doubleValue();

        List<Map<String, Object>> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] data : salesPerDay) {
            LocalDate day = (LocalDate) data[0];
            BigDecimal value = asBigDecimal(data[1]);
            double pct = maxDouble > 0 ? value.doubleValue() * 100.0 / maxDouble : 0d;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", rank++);
            row.put("dateText", DATE_LONG_ID.format(day));
            row.put("amountText", formatRupiah(value));
            row.put("pct", round2(pct));
            rows.add(row);
        }
        return rows;
    }

    /* ============================================================
       FORMAT & KONVERSI
       ============================================================ */

    /** Angka ribuan gaya Indonesia, contoh: 1.234.567 */
    public static String formatNumber(long value) {
        DecimalFormat df = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(LOCALE_ID));
        return df.format(value);
    }

    /** Uang gaya Indonesia, contoh: Rp 1.234.567 */
    public static String formatRupiah(BigDecimal value) {
        DecimalFormat df = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(LOCALE_ID));
        return "Rp " + df.format(value == null ? BigDecimal.ZERO : value);
    }

    /** Persentase gaya Indonesia, contoh: 33,3% */
    public static String formatPercent(double value) {
        DecimalFormat df = new DecimalFormat("#,##0.0", DecimalFormatSymbols.getInstance(LOCALE_ID));
        return df.format(value) + "%";
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private static long sumLongColumn(List<Object[]> rows, int columnIndex) {
        long total = 0;
        for (Object[] row : rows) {
            if (row[columnIndex] instanceof Number number) {
                total += number.longValue();
            }
        }
        return total;
    }

    private static BigDecimal sumDecimalColumn(List<Object[]> rows, int columnIndex) {
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] row : rows) {
            if (row[columnIndex] instanceof BigDecimal decimal) {
                total = total.add(decimal);
            } else if (row[columnIndex] instanceof Number number) {
                total = total.add(BigDecimal.valueOf(number.doubleValue()));
            }
        }
        return total;
    }
}
