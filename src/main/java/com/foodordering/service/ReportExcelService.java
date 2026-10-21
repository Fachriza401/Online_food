package com.foodordering.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.foodordering.model.OrderStatus;
import com.foodordering.model.RestaurantSetting;

/**
 * Membuat file Excel (.xlsx) laporan penjualan dengan tampilan modern
 * enterprise: kop surat rapi, header tabel berkontras tinggi, garis tabel
 * minimalis (hanya garis bawah), baris TOTAL menonjol, dan format Rupiah.
 *
 * <p>Strukturnya sengaja dibuat "\u201cdashboard\u201d" (bukan grid Excel kaku) agar
 * file export terasa konsisten dengan tampilan laporan di aplikasi.</p>
 */
@Service
public class ReportExcelService {

    private static final DateTimeFormatter DATE_LONG =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new Locale("id", "ID"));
    private static final String RUPIAH_FORMAT = "\"Rp\" #,##0";

    /* Palet warna laporan (mengikuti tema aplikasi) */
    private static final byte[] INK = rgb(0x1C, 0x19, 0x17);
    private static final byte[] BRAND = rgb(0xF9, 0x73, 0x16);
    private static final byte[] MUTED = rgb(0x78, 0x71, 0x6C);
    private static final byte[] LINE = rgb(0xE7, 0xE5, 0xE4);
    private static final byte[] SOFT = rgb(0xF5, 0xF5, 0xF4);
    private static final byte[] WHITE = rgb(0xFF, 0xFF, 0xFF);

    private final ReportService reportService;
    private final RestaurantSettingService restaurantSettingService;

    public ReportExcelService(ReportService reportService,
                              RestaurantSettingService restaurantSettingService) {
        this.reportService = reportService;
        this.restaurantSettingService = restaurantSettingService;
    }

    /** Byte[] .xlsx siap ditulis ke response. */
    public byte[] buildLaporanPenjualan() throws IOException {
        RestaurantSetting restaurant = restaurantSettingService.get();
        Map<String, Object> stats = reportService.dashboardStats();
        List<Object[]> topFoods = reportService.topFoods(20);
        Map<OrderStatus, Long> ordersByStatus = reportService.ordersByStatus();
        List<Object[]> salesPerDay = reportService.salesPerDay(30);

        try (Workbook wb = new XSSFWorkbook()) {
            Styles st = new Styles((XSSFWorkbook) wb);

            buildSheetRingkasan(wb, st, restaurant, stats);
            buildSheetMenuTerlaris(wb, st, restaurant, topFoods);
            buildSheetStatus(wb, st, restaurant, ordersByStatus);
            buildSheetPenjualanHarian(wb, st, restaurant, salesPerDay);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        }
    }

    /* ============================================================
       SHEET 1 — RINGKASAN (tampilan dashboard / kartu KPI)
       ============================================================ */
    private void buildSheetRingkasan(Workbook wb, Styles st, RestaurantSetting r,
                                     Map<String, Object> stats) {
        Sheet sheet = wb.createSheet("Ringkasan");
        int row = writeKopLaporan(sheet, st, r, "LAPORAN PENJUALAN", "Ringkasan Umum");
        row++;

        writeSectionTitle(sheet, st, row, "RINGKASAN UTAMA", 2);
        row += 2;

        writeLabelValue(sheet, st, row++, "Total Pendapatan",
                asBigDecimal(stats.get("revenue")), true);
        writeLabelValue(sheet, st, row++, "Pesanan Diterima",
                asLong(stats.get("orderCount")), false);
        writeLabelValue(sheet, st, row++, "Pesanan Menunggu Konfirmasi",
                asLong(stats.get("pendingCount")), false);
        writeLabelValue(sheet, st, row++, "Pelanggan Terdaftar",
                asLong(stats.get("customerCount")), false);
        writeLabelValue(sheet, st, row++, "Menu Tersedia",
                asLong(stats.get("menuCount")), false);

        row++;
        writeSectionTitle(sheet, st, row, "METODE PEMBAYARAN", 2);
        row += 2;
        writeLabelValue(sheet, st, row++, "Metode Pembayaran Aktif",
                com.foodordering.model.PaymentMethod.COD.getLabel() + " (Bayar di Tempat)", false);

        writeFooterTandaTangan(sheet, st, row + 3, r);
        finishSheet(sheet, 26, 4, 26);
    }

    /* ============================================================
       SHEET 2 — MENU TERLARIS
       ============================================================ */
    private void buildSheetMenuTerlaris(Workbook wb, Styles st, RestaurantSetting r,
                                        List<Object[]> topFoods) {
        Sheet sheet = wb.createSheet("Menu Terlaris");
        int row = writeKopLaporan(sheet, st, r,
                "LAPORAN PENJUALAN", "Menu Terlaris (Berdasarkan Jumlah Terjual)");
        row++;

        int headerRow = row;
        writeHeaderRow(sheet, st, row, "No", "Menu", "Terjual", "Omzet");
        row++;

        long totalTerjual = 0;
        BigDecimal totalOmzet = BigDecimal.ZERO;
        int no = 1;
        for (Object[] data : topFoods) {
            String nama = String.valueOf(data[0]);
            long terjual = asLong(data[1]);
            BigDecimal omzet = asBigDecimal(data[2]);
            totalTerjual += terjual;
            totalOmzet = totalOmzet.add(omzet);

            Row r0 = sheet.createRow(row);
            Cell cNo = r0.createCell(0);
            cNo.setCellValue(no++);
            cNo.setCellStyle(st.cellNum);
            Cell cNama = r0.createCell(1);
            cNama.setCellValue(nama);
            cNama.setCellStyle(st.cell);
            Cell cJual = r0.createCell(2);
            cJual.setCellValue(terjual);
            cJual.setCellStyle(st.cellNum);
            Cell cOmzet = r0.createCell(3);
            cOmzet.setCellValue(omzet.doubleValue());
            cOmzet.setCellStyle(st.cellRupiah);
            row++;
        }
        if (topFoods.isEmpty()) {
            Row r0 = sheet.createRow(row);
            Cell c = r0.createCell(0);
            c.setCellValue("Belum ada penjualan.");
            c.setCellStyle(st.cell);
            sheet.addMergedRegion(new CellRangeAddress(row, row, 0, 3));
            row++;
        } else {
            Row t = sheet.createRow(row);
            Cell tLabel = t.createCell(0);
            tLabel.setCellValue("TOTAL");
            tLabel.setCellStyle(st.totalText);
            t.createCell(1).setCellStyle(st.totalText);
            Cell tJual = t.createCell(2);
            tJual.setCellValue(totalTerjual);
            tJual.setCellStyle(st.totalNum);
            Cell tOmzet = t.createCell(3);
            tOmzet.setCellValue(totalOmzet.doubleValue());
            tOmzet.setCellStyle(st.totalRupiah);
            row++;
        }

        writeFooterTandaTangan(sheet, st, row + 3, r);
        finishSheet(sheet, headerRow, 5, 34, 14, 22);
    }

    /* ============================================================
       SHEET 3 — DISTRIBUSI STATUS PESANAN
       ============================================================ */
    private void buildSheetStatus(Workbook wb, Styles st, RestaurantSetting r,
                                  Map<OrderStatus, Long> ordersByStatus) {
        Sheet sheet = wb.createSheet("Status Pesanan");
        int row = writeKopLaporan(sheet, st, r, "LAPORAN PENJUALAN",
                "Distribusi Status Pesanan");
        row++;

        int headerRow = row;
        writeHeaderRow(sheet, st, row, "No", "Status", "Jumlah", "Porsi");
        row++;

        long total = 0;
        int no = 1;
        for (Map.Entry<OrderStatus, Long> entry : ordersByStatus.entrySet()) {
            total += entry.getValue();
            Row r0 = sheet.createRow(row);
            Cell cNo = r0.createCell(0);
            cNo.setCellValue(no++);
            cNo.setCellStyle(st.cellNum);
            Cell cStatus = r0.createCell(1);
            cStatus.setCellValue(entry.getKey().getLabel());
            cStatus.setCellStyle(st.cell);
            Cell cJml = r0.createCell(2);
            cJml.setCellValue(entry.getValue());
            cJml.setCellStyle(st.cellNum);
            Cell cPorsi = r0.createCell(3);
            double pct = total > 0 ? entry.getValue() * 100.0 / total : 0d;
            cPorsi.setCellValue(pct);
            cPorsi.setCellStyle(st.cellPercent);
            row++;
        }

        Row t = sheet.createRow(row);
        Cell tLabel = t.createCell(0);
        tLabel.setCellValue("TOTAL");
        tLabel.setCellStyle(st.totalText);
        t.createCell(1).setCellStyle(st.totalText);
        Cell tJml = t.createCell(2);
        tJml.setCellValue(total);
        tJml.setCellStyle(st.totalNum);
        Cell tPorsi = t.createCell(3);
        tPorsi.setCellValue(100d);
        tPorsi.setCellStyle(st.totalPercent);
        row += 3;

        Row pay = sheet.createRow(row);
        Cell payCell = pay.createCell(0);
        payCell.setCellValue("Metode pembayaran aktif: "
                + com.foodordering.model.PaymentMethod.COD.getLabel() + " (Bayar di Tempat).");
        payCell.setCellStyle(st.note);
        sheet.addMergedRegion(new CellRangeAddress(row, row, 0, 3));

        writeFooterTandaTangan(sheet, st, row + 4, r);
        finishSheet(sheet, headerRow, 5, 30, 14, 14);
    }

    /* ============================================================
       SHEET 4 — PENJUALAN HARIAN (30 HARI)
       ============================================================ */
    private void buildSheetPenjualanHarian(Workbook wb, Styles st, RestaurantSetting r,
                                           List<Object[]> salesPerDay) {
        Sheet sheet = wb.createSheet("Penjualan Harian");
        LocalDate today = LocalDate.now();
        String periode = "Periode: " + DATE_LONG.format(today.minusDays(29))
                + " s.d. " + DATE_LONG.format(today);
        int row = writeKopLaporan(sheet, st, r, "LAPORAN PENJUALAN",
                "Penjualan 30 Hari Terakhir (per-hari)", periode);
        row++;

        int headerRow = row;
        writeHeaderRow(sheet, st, row, "No", "Tanggal", "Total Penjualan");
        row++;

        BigDecimal total = BigDecimal.ZERO;
        int no = 1;
        for (Object[] data : salesPerDay) {
            LocalDate day = (LocalDate) data[0];
            BigDecimal nilai = asBigDecimal(data[1]);
            total = total.add(nilai);

            Row r0 = sheet.createRow(row);
            Cell cNo = r0.createCell(0);
            cNo.setCellValue(no++);
            cNo.setCellStyle(st.cellNum);
            Cell cTgl = r0.createCell(1);
            cTgl.setCellValue(DATE_LONG.format(day));
            cTgl.setCellStyle(st.cell);
            Cell cNilai = r0.createCell(2);
            cNilai.setCellValue(nilai.doubleValue());
            cNilai.setCellStyle(st.cellRupiah);
            row++;
        }
        if (salesPerDay.isEmpty()) {
            Row r0 = sheet.createRow(row);
            Cell c = r0.createCell(0);
            c.setCellValue("Belum ada data penjualan.");
            c.setCellStyle(st.cell);
            sheet.addMergedRegion(new CellRangeAddress(row, row, 0, 2));
            row++;
        } else {
            Row t = sheet.createRow(row);
            Cell tLabel = t.createCell(0);
            tLabel.setCellValue("TOTAL");
            tLabel.setCellStyle(st.totalText);
            t.createCell(1).setCellStyle(st.totalText);
            Cell tNilai = t.createCell(2);
            tNilai.setCellValue(total.doubleValue());
            tNilai.setCellStyle(st.totalRupiah);
            row++;
        }

        writeFooterTandaTangan(sheet, st, row + 3, r);
        finishSheet(sheet, headerRow, 5, 26, 24);
    }

    /* ============================================================
       HELPER — kop, header tabel, footer, gaya
       ============================================================ */

    /** Menulis kop laporan (nama restoran, alamat, judul). Mengembalikan baris berikutnya. */
    private int writeKopLaporan(Sheet sheet, Styles st, RestaurantSetting r,
                                String judul, String subJudul) {
        return writeKopLaporan(sheet, st, r, judul, subJudul, null);
    }

    private int writeKopLaporan(Sheet sheet, Styles st, RestaurantSetting r,
                                String judul, String subJudul, String periode) {
        int row = 0;

        Row nama = sheet.createRow(row++);
        Cell cNama = nama.createCell(0);
        cNama.setCellValue(r.resolveName().toUpperCase());
        cNama.setCellStyle(st.kop);
        sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 3));

        if (r.getAddress() != null && !r.getAddress().isBlank()) {
            Row alamat = sheet.createRow(row++);
            Cell cAlamat = alamat.createCell(0);
            cAlamat.setCellValue(r.getAddress());
            cAlamat.setCellStyle(st.kopAlamat);
            sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 3));
        }
        String kontak = joinNonBlank(
                r.getPhone() != null ? "Telp. " + r.getPhone() : null,
                r.getEmail() != null ? "Email: " + r.getEmail() : null);
        if (!kontak.isBlank()) {
            Row telp = sheet.createRow(row++);
            Cell cTelp = telp.createCell(0);
            cTelp.setCellValue(kontak);
            cTelp.setCellStyle(st.kopAlamat);
            sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 3));
        }

        row++; // baris kosong

        Row j = sheet.createRow(row++);
        Cell cJudul = j.createCell(0);
        cJudul.setCellValue(judul);
        cJudul.setCellStyle(st.judul);
        sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 3));

        Row s = sheet.createRow(row++);
        Cell cSub = s.createCell(0);
        cSub.setCellValue(subJudul);
        cSub.setCellStyle(st.subJudul);
        sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 3));

        if (periode != null) {
            Row p = sheet.createRow(row++);
            Cell cP = p.createCell(0);
            cP.setCellValue(periode);
            cP.setCellStyle(st.subJudul);
            sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 3));
        }

        Row dicetak = sheet.createRow(row++);
        Cell cDicetak = dicetak.createCell(0);
        cDicetak.setCellValue("Dicetak pada " + DATE_TIME.format(LocalDateTime.now()));
        cDicetak.setCellStyle(st.kopAlamat);
        sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 3));

        row++; // baris kosong sebelum tabel
        return row;
    }

    /** Judul bagian (garis aksen oranye di bawahnya). */
    private void writeSectionTitle(Sheet sheet, Styles st, int rowIndex, String title, int lastCol) {
        Row row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(st.sectionTitle);
        for (int i = 1; i <= lastCol; i++) {
            row.createCell(i).setCellStyle(st.sectionTitle);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastCol));
    }

    /** Baris label + nilai pada sheet Ringkasan (label merge A:B / nilai di kolom C). */
    private void writeLabelValue(Sheet sheet, Styles st, int rowIndex,
                                 String label, Object value, boolean rupiah) {
        Row row = sheet.createRow(rowIndex);
        Cell cLabel = row.createCell(0);
        cLabel.setCellValue(label);
        cLabel.setCellStyle(st.kpiLabel);
        row.createCell(1).setCellStyle(st.kpiLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 1));

        Cell cValue = row.createCell(2);
        if (value instanceof Number number) {
            if (rupiah) {
                cValue.setCellValue(number.doubleValue());
                cValue.setCellStyle(st.kpiValueRupiah);
            } else {
                cValue.setCellValue(number.doubleValue());
                cValue.setCellStyle(st.kpiValue);
            }
        } else {
            cValue.setCellValue(String.valueOf(value));
            cValue.setCellStyle(st.kpiValueText);
        }
    }

    /** Header tabel multi kolom: No | ... */
    private void writeHeaderRow(Sheet sheet, Styles st, int rowIndex, String... labels) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(20f);
        for (int i = 0; i < labels.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(labels[i]);
            cell.setCellStyle(st.header);
        }
    }

    /** Blok tanda tangan di pojok kanan bawah. */
    private void writeFooterTandaTangan(Sheet sheet, Styles st, int rowIndex,
                                         RestaurantSetting r) {
        Row tgl = sheet.createRow(rowIndex);
        Cell cTgl = tgl.createCell(0);
        cTgl.setCellValue(DATE_LONG.format(LocalDate.now()));
        cTgl.setCellStyle(st.sigCenter);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 2, 3));

        Row peran = sheet.createRow(rowIndex + 1);
        Cell cPeran = peran.createCell(0);
        cPeran.setCellValue("Admin " + r.resolveName());
        cPeran.setCellStyle(st.sigCenter);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 1, rowIndex + 1, 2, 3));

        // baris kosong untuk tanda tangan
        Row space = sheet.createRow(rowIndex + 2);
        space.setHeightInPoints(34);

        Row nama = sheet.createRow(rowIndex + 3);
        Cell cNama = nama.createCell(0);
        cNama.setCellValue("(                              )");
        cNama.setCellStyle(st.sigCenter);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex + 3, rowIndex + 3, 2, 3));
    }

    /** Set lebar kolom, kunci baris header, dan repeating row untuk cetak. */
    private void finishSheet(Sheet sheet, int headerRowIndex, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        sheet.createFreezePane(0, headerRowIndex + 1);
        int repeat = headerRowIndex + 1;
        sheet.setRepeatingRows(CellRangeAddress.valueOf(repeat + ":" + repeat));
    }

    /* ============================================================
       KONVERSI & UTIL
       ============================================================ */
    private long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (sb.length() > 0) {
                    sb.append("  |  ");
                }
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }

    private static byte[] rgb(int r, int g, int b) {
        return new byte[]{ (byte) r, (byte) g, (byte) b };
    }

    /* ============================================================
       KUMPULAN CELLSTYLE (tampilan modern: garis minimalis)
       ============================================================ */
    private static final class Styles {
        final XSSFCellStyle kop;
        final XSSFCellStyle kopAlamat;
        final XSSFCellStyle judul;
        final XSSFCellStyle subJudul;
        final XSSFCellStyle sectionTitle;
        final XSSFCellStyle header;
        final XSSFCellStyle cell;
        final XSSFCellStyle cellNum;
        final XSSFCellStyle cellRupiah;
        final XSSFCellStyle cellPercent;
        final XSSFCellStyle totalText;
        final XSSFCellStyle totalNum;
        final XSSFCellStyle totalRupiah;
        final XSSFCellStyle totalPercent;
        final XSSFCellStyle kpiLabel;
        final XSSFCellStyle kpiValue;
        final XSSFCellStyle kpiValueRupiah;
        final XSSFCellStyle kpiValueText;
        final XSSFCellStyle note;
        final XSSFCellStyle sigCenter;

        Styles(XSSFWorkbook wb) {
            XSSFColor cInk = color(INK);
            XSSFColor cBrand = color(BRAND);
            XSSFColor cMuted = color(MUTED);
            XSSFColor cLine = color(LINE);
            XSSFColor cSoft = color(SOFT);
            XSSFColor cWhite = color(WHITE);

            XSSFFont fontKop = font(wb, 14, true, cInk);
            XSSFFont fontAlamat = font(wb, 9, false, cMuted);
            XSSFFont fontJudul = font(wb, 12, true, cInk);
            XSSFFont fontSub = font(wb, 10, false, cMuted);
            XSSFFont fontSection = font(wb, 10, true, cBrand);
            XSSFFont fontHeader = font(wb, 10, true, cWhite);
            XSSFFont fontBody = font(wb, 10, false, cInk);
            XSSFFont fontTotal = font(wb, 10, true, cInk);
            XSSFFont fontKpiLabel = font(wb, 10, true, cMuted);
            XSSFFont fontKpiValue = font(wb, 12, true, cInk);
            XSSFFont fontNote = font(wb, 9, false, cMuted);
            fontNote.setItalic(true);

            // --- Kop ---
            kop = base(wb, fontKop, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            kopAlamat = base(wb, fontAlamat, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            judul = base(wb, fontJudul, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            subJudul = base(wb, fontSub, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            sectionTitle = base(wb, fontSection, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            bottom(sectionTitle, cBrand, BorderStyle.MEDIUM);

            // --- Header tabel: band gelap + aksen oranye ---
            header = base(wb, fontHeader, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            fill(header, cInk);
            bottom(header, cBrand, BorderStyle.MEDIUM);

            // --- Baris isi: hanya garis bawah tipis ---
            cell = base(wb, fontBody, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            bottom(cell, cLine, BorderStyle.THIN);

            cellNum = base(wb, fontBody, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);
            bottom(cellNum, cLine, BorderStyle.THIN);
            cellNum.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

            cellRupiah = base(wb, fontBody, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);
            bottom(cellRupiah, cLine, BorderStyle.THIN);
            cellRupiah.setDataFormat(wb.createDataFormat().getFormat(RUPIAH_FORMAT));

            cellPercent = base(wb, fontBody, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);
            bottom(cellPercent, cLine, BorderStyle.THIN);
            cellPercent.setDataFormat(wb.createDataFormat().getFormat("0.0\"%\""));

            // --- Baris TOTAL ---
            totalText = base(wb, fontTotal, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            fill(totalText, cSoft);
            top(totalText, cInk, BorderStyle.MEDIUM);

            totalNum = base(wb, fontTotal, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);
            fill(totalNum, cSoft);
            top(totalNum, cInk, BorderStyle.MEDIUM);
            totalNum.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

            totalRupiah = base(wb, fontTotal, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);
            fill(totalRupiah, cSoft);
            top(totalRupiah, cInk, BorderStyle.MEDIUM);
            totalRupiah.setDataFormat(wb.createDataFormat().getFormat(RUPIAH_FORMAT));

            totalPercent = base(wb, fontTotal, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);
            fill(totalPercent, cSoft);
            top(totalPercent, cInk, BorderStyle.MEDIUM);
            totalPercent.setDataFormat(wb.createDataFormat().getFormat("0.0\"%\""));

            // --- Kartu KPI pada sheet ringkasan ---
            kpiLabel = base(wb, fontKpiLabel, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            fill(kpiLabel, cSoft);

            kpiValue = base(wb, fontKpiValue, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);
            kpiValue.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

            kpiValueRupiah = base(wb, fontKpiValue, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);
            kpiValueRupiah.setDataFormat(wb.createDataFormat().getFormat(RUPIAH_FORMAT));

            kpiValueText = base(wb, fontBody, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);

            note = base(wb, fontNote, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            sigCenter = base(wb, fontBody, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        }

        private static XSSFColor color(byte[] rgb) {
            return new XSSFColor(rgb, null);
        }

        private static XSSFFont font(XSSFWorkbook wb, int size, boolean bold, XSSFColor color) {
            XSSFFont f = wb.createFont();
            f.setBold(bold);
            f.setFontHeightInPoints((short) size);
            f.setFontName("Calibri");
            f.setColor(color);
            return f;
        }

        private static XSSFCellStyle base(XSSFWorkbook wb, XSSFFont font,
                                          HorizontalAlignment h, VerticalAlignment v) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setFont(font);
            cs.setAlignment(h);
            cs.setVerticalAlignment(v);
            return cs;
        }

        private static void fill(XSSFCellStyle cs, XSSFColor color) {
            cs.setFillForegroundColor(color);
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        private static void bottom(XSSFCellStyle cs, XSSFColor color, BorderStyle style) {
            cs.setBorderBottom(style);
            cs.setBottomBorderColor(color);
        }

        private static void top(XSSFCellStyle cs, XSSFColor color, BorderStyle style) {
            cs.setBorderTop(style);
            cs.setTopBorderColor(color);
        }
    }
}
