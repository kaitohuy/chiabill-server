package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.response.SettlementResponse;
import com.kaitohuy.chiabill.entity.*;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.repository.*;
import com.kaitohuy.chiabill.service.interfaces.ExportService;
import com.kaitohuy.chiabill.service.interfaces.SettlementService;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementService settlementService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color COLOR_PRIMARY = new Color(33, 150, 243);
    private static final Color COLOR_HEADER_TEXT = Color.WHITE;
    private static final Color COLOR_ROW_ALT = new Color(240, 248, 255);

    // Dữ liệu dùng chung cho cả Excel lẫn PDF
    private record ExportContext(Trip trip, List<TripMember> members,
                                 List<com.kaitohuy.chiabill.dto.response.CategoryStatResponse> stats) {}

    private ExportContext loadExportContext(Long tripId, Long userId) {
        validateAccess(tripId, userId);
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new BusinessException("Trip not found"));
        List<TripMember> members = tripMemberRepository.findActiveMembersWithUser(tripId);
        var stats = expenseRepository.getExpenseStatsByCategory(tripId);
        return new ExportContext(trip, members, stats);
    }

    // ─────────────────────────────────────────
    // EXCEL — Cache styles 1 lần duy nhất
    // ─────────────────────────────────────────
    private record Styles(XSSFCellStyle title, XSSFCellStyle section, XSSFCellStyle colHeader,
                          XSSFCellStyle total, XSSFCellStyle label, XSSFCellStyle evenRow, XSSFCellStyle oddRow) {}

    private Styles createStyles(XSSFWorkbook wb) {
        XSSFCellStyle title = wb.createCellStyle();
        XSSFFont tf = wb.createFont(); tf.setBold(true); tf.setFontHeightInPoints((short) 16);
        tf.setColor(new XSSFColor(COLOR_PRIMARY, null));
        title.setFont(tf); title.setAlignment(HorizontalAlignment.CENTER); title.setVerticalAlignment(VerticalAlignment.CENTER);

        XSSFCellStyle section = wb.createCellStyle();
        section.setFillForegroundColor(new XSSFColor(COLOR_PRIMARY, null));
        section.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont sf = wb.createFont(); sf.setBold(true); sf.setFontHeightInPoints((short) 11);
        sf.setColor(new XSSFColor(COLOR_HEADER_TEXT, null));
        section.setFont(sf); section.setAlignment(HorizontalAlignment.LEFT); section.setVerticalAlignment(VerticalAlignment.CENTER);

        XSSFCellStyle colHeader = wb.createCellStyle();
        colHeader.setFillForegroundColor(new XSSFColor(new Color(187, 222, 251), null));
        colHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont cf = wb.createFont(); cf.setBold(true);
        colHeader.setFont(cf); colHeader.setAlignment(HorizontalAlignment.CENTER);
        colHeader.setBorderBottom(BorderStyle.MEDIUM); colHeader.setBorderTop(BorderStyle.THIN);

        XSSFCellStyle total = wb.createCellStyle();
        total.setFillForegroundColor(new XSSFColor(new Color(220, 237, 255), null));
        total.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont tof = wb.createFont(); tof.setBold(true);
        total.setFont(tof); total.setBorderTop(BorderStyle.MEDIUM);

        XSSFCellStyle label = wb.createCellStyle();
        XSSFFont lf = wb.createFont(); lf.setBold(true); label.setFont(lf);

        XSSFCellStyle evenRow = wb.createCellStyle();
        evenRow.setFillForegroundColor(new XSSFColor(COLOR_ROW_ALT, null));
        evenRow.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        evenRow.setBorderBottom(BorderStyle.THIN); evenRow.setBorderTop(BorderStyle.THIN);

        XSSFCellStyle oddRow = wb.createCellStyle();
        oddRow.setBorderBottom(BorderStyle.THIN); oddRow.setBorderTop(BorderStyle.THIN);

        return new Styles(title, section, colHeader, total, label, evenRow, oddRow);
    }

    @Override
    public byte[] exportTripToExcel(Long tripId, Long userId, boolean includeDetails, boolean includeSettlement) {
        ExportContext ctx = loadExportContext(tripId, userId);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles s = createStyles(wb);
            buildOverviewSheet(wb, s, ctx.trip(), ctx.members(), ctx.stats());
            if (includeDetails) {
                buildDetailsSheet(wb, s, ctx.trip(), expenseRepository.findAllByTripIdWithPayerAndCategory(tripId));
            }
            if (includeSettlement) {
                buildSettlementSheet(wb, s, settlementService.calculateSettlement(tripId, userId));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Excel: ", e);
            throw new BusinessException("Lỗi khi xuất file Excel");
        }
    }

    private void buildOverviewSheet(XSSFWorkbook wb, Styles s, Trip trip, List<TripMember> members,
                                     List<com.kaitohuy.chiabill.dto.response.CategoryStatResponse> stats) {
        XSSFSheet sheet = wb.createSheet("Tong quan");
        int row = 0;

        XSSFRow titleRow = sheet.createRow(row++);
        titleRow.setHeightInPoints(36);
        XSSFCell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO CHUYẾN ĐI: " + trip.getName().toUpperCase());
        titleCell.setCellStyle(s.title());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        row++;

        createInfoRow(sheet, s, row++, "Mô tả:", trip.getDescription() != null ? trip.getDescription() : "N/A");
        createInfoRow(sheet, s, row++, "Ngân sách:", trip.getTotalBudget() != null ? formatMoney(trip.getTotalBudget()) + " " + trip.getCurrency() : "Chưa đặt");
        createInfoRow(sheet, s, row++, "Ngày xuất:", LocalDateTime.now().format(DATETIME_FMT));
        row++;

        row = writeHeaderRow(sheet, s, row, "DANH SÁCH THÀNH VIÊN", new String[]{"STT", "Họ tên", "SĐT", "Email", "Vai trò"}, 5);
        int stt = 1;
        for (TripMember m : members) {
            XSSFRow r = sheet.createRow(row++);
            XSSFCellStyle rs = stt % 2 == 0 ? s.evenRow() : s.oddRow();
            for (int c = 0; c < 5; c++) r.createCell(c).setCellStyle(rs);
            r.getCell(0).setCellValue(stt++);
            r.getCell(1).setCellValue(m.getUser().getName());
            r.getCell(2).setCellValue(m.getUser().getPhone() != null ? m.getUser().getPhone() : "");
            r.getCell(3).setCellValue(m.getUser().getEmail() != null ? m.getUser().getEmail() : "");
            r.getCell(4).setCellValue("OWNER".equals(m.getRole()) ? "Chủ phòng" : "Thành viên");
        }
        row++;

        row = writeHeaderRow(sheet, s, row, "TỔNG HỢP THEO DANH MỤC", new String[]{"Danh mục", "Tổng chi", "Đơn vị", "Tỉ lệ"}, 4);
        BigDecimal grandTotal = stats.stream().map(st -> st.getTotalAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        int idx = 1;
        for (var stat : stats) {
            XSSFRow r = sheet.createRow(row++);
            XSSFCellStyle rs = idx % 2 == 0 ? s.evenRow() : s.oddRow();
            for (int c = 0; c < 4; c++) r.createCell(c).setCellStyle(rs);
            r.getCell(0).setCellValue(stat.getCategoryName());
            r.getCell(1).setCellValue(stat.getTotalAmount().doubleValue());
            r.getCell(2).setCellValue(trip.getCurrency());
            double pct = grandTotal.compareTo(BigDecimal.ZERO) > 0 ? stat.getTotalAmount().doubleValue() / grandTotal.doubleValue() * 100 : 0;
            r.getCell(3).setCellValue(String.format("%.1f%%", pct));
            idx++;
        }
        XSSFRow totalRow = sheet.createRow(row);
        for (int c = 0; c < 4; c++) totalRow.createCell(c).setCellStyle(s.total());
        totalRow.getCell(0).setCellValue("TỔNG CỘNG");
        totalRow.getCell(1).setCellValue(grandTotal.doubleValue());
        totalRow.getCell(2).setCellValue(trip.getCurrency());

        for (int c = 0; c < 6; c++) sheet.autoSizeColumn(c);
    }

    private void buildDetailsSheet(XSSFWorkbook wb, Styles s, Trip trip, List<Expense> expenses) {
        XSSFSheet sheet = wb.createSheet("Chi tiet chi phi");
        String[] cols = {"Ngày", "Tên khoản chi", "Danh mục", "Người trả", "Số tiền", "Đơn vị"};
        int row = writeHeaderRow(sheet, s, 0, "CHI TIẾT TỪNG KHOẢN CHI", cols, cols.length);
        int idx = 1;
        for (Expense e : expenses) {
            XSSFRow r = sheet.createRow(row++);
            XSSFCellStyle rs = idx % 2 == 0 ? s.evenRow() : s.oddRow();
            for (int c = 0; c < cols.length; c++) r.createCell(c).setCellStyle(rs);
            r.getCell(0).setCellValue(e.getExpenseDate() != null ? e.getExpenseDate().format(DATE_FMT) : "");
            r.getCell(1).setCellValue(e.getDescription() != null ? e.getDescription() : "");
            r.getCell(2).setCellValue(e.getCategory() != null ? e.getCategory().getName() : "");
            r.getCell(3).setCellValue(e.getPayer() != null ? e.getPayer().getName() : "");
            r.getCell(4).setCellValue(e.getTotalAmount() != null ? e.getTotalAmount().doubleValue() : 0);
            r.getCell(5).setCellValue(e.getCurrency() != null ? e.getCurrency() : trip.getCurrency());
            idx++;
        }
        for (int c = 0; c < cols.length; c++) sheet.autoSizeColumn(c);
    }

    private void buildSettlementSheet(XSSFWorkbook wb, Styles s, List<SettlementResponse> debts) {
        XSSFSheet sheet = wb.createSheet("Quyet toan no");
        String[] cols = {"Người nợ", "Người nhận", "Số tiền cần trả"};
        int row = writeHeaderRow(sheet, s, 0, "BẢNG QUYẾT TOÁN NỢ", cols, cols.length);
        int idx = 1;
        for (SettlementResponse d : debts) {
            XSSFRow r = sheet.createRow(row++);
            XSSFCellStyle rs = idx % 2 == 0 ? s.evenRow() : s.oddRow();
            for (int c = 0; c < cols.length; c++) r.createCell(c).setCellStyle(rs);
            r.getCell(0).setCellValue(d.getFromUserName());
            r.getCell(1).setCellValue(d.getToUserName());
            r.getCell(2).setCellValue(d.getAmount().doubleValue());
            idx++;
        }
        if (debts.isEmpty()) {
            sheet.createRow(row).createCell(0).setCellValue("Không có khoản nợ nào!");
        }
        for (int c = 0; c < cols.length; c++) sheet.autoSizeColumn(c);
    }

    private void createInfoRow(XSSFSheet sheet, Styles s, int rowIdx, String label, String value) {
        XSSFRow row = sheet.createRow(rowIdx);
        XSSFCell lc = row.createCell(0); lc.setCellValue(label); lc.setCellStyle(s.label());
        row.createCell(1).setCellValue(value);
    }

    private int writeHeaderRow(XSSFSheet sheet, Styles s, int rowIdx, String sectionTitle, String[] cols, int colCount) {
        XSSFRow secRow = sheet.createRow(rowIdx++);
        secRow.setHeightInPoints(22);
        XSSFCell secCell = secRow.createCell(0);
        secCell.setCellValue(sectionTitle);
        secCell.setCellStyle(s.section());
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, colCount - 1));

        XSSFRow hRow = sheet.createRow(rowIdx++);
        hRow.setHeightInPoints(18);
        for (int i = 0; i < cols.length; i++) {
            XSSFCell c = hRow.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(s.colHeader());
        }
        return rowIdx;
    }

    // ─────────────────────────────────────────
    // PDF EXPORT
    // ─────────────────────────────────────────
    @Override
    public byte[] exportTripToPdf(Long tripId, Long userId, boolean includeDetails, boolean includeSettlement) {
        ExportContext ctx = loadExportContext(tripId, userId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Load Arial font từ classpath để hỗ trợ tiếng Việt
            BaseFont baseFont;
            BaseFont baseFontBold;
            try (InputStream is = getClass().getResourceAsStream("/fonts/arial.ttf");
                 InputStream isB = getClass().getResourceAsStream("/fonts/arialbd.ttf")) {
                if (is != null) {
                    baseFont = BaseFont.createFont("arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, is.readAllBytes(), null);
                } else {
                    baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                }
                if (isB != null) {
                    baseFontBold = BaseFont.createFont("arialbd.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, isB.readAllBytes(), null);
                } else {
                    baseFontBold = baseFont;
                }
            }

            Font titleFont = new Font(baseFontBold, 18, Font.BOLD, COLOR_PRIMARY);
            Font sectionFont = new Font(baseFontBold, 12, Font.BOLD, Color.WHITE);
            Font boldFont = new Font(baseFontBold, 10, Font.BOLD);
            Font normalFont = new Font(baseFont, 9, Font.NORMAL);
            Font smallGrey = new Font(baseFont, 8, Font.NORMAL, Color.GRAY);

            // Title
            Paragraph title = new Paragraph("BÁO CÁO CHUYẾN ĐI\n" + ctx.trip().getName().toUpperCase(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            doc.add(title);
            Paragraph sub = new Paragraph("Xuất ngày: " + LocalDateTime.now().format(DATETIME_FMT), smallGrey);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(18);
            doc.add(sub);

            // Info
            addSectionHeader(doc, sectionFont, "THÔNG TIN CHUYẾN ĐI");
            doc.add(new Paragraph("Mô tả: " + (ctx.trip().getDescription() != null ? ctx.trip().getDescription() : "N/A"), normalFont));
            doc.add(new Paragraph("Ngân sách: " + (ctx.trip().getTotalBudget() != null ? formatMoney(ctx.trip().getTotalBudget()) + " " + ctx.trip().getCurrency() : "Chưa đặt"), normalFont));
            doc.add(new Paragraph(" ", normalFont));

            // Members
            addSectionHeader(doc, sectionFont, "DANH SÁCH THÀNH VIÊN");
            PdfPTable memberTable = new PdfPTable(new float[]{1, 3, 2, 3, 2});
            memberTable.setWidthPercentage(100);
            memberTable.setSpacingAfter(12);
            for (String h : new String[]{"STT", "Họ tên", "SĐT", "Email", "Vai trò"}) addPdfHeaderCell(memberTable, h, boldFont);
            int stt = 1;
            for (TripMember m : ctx.members()) {
                boolean alt = stt % 2 == 0;
                addPdfDataCell(memberTable, String.valueOf(stt++), normalFont, alt);
                addPdfDataCell(memberTable, m.getUser().getName(), normalFont, alt);
                addPdfDataCell(memberTable, m.getUser().getPhone() != null ? m.getUser().getPhone() : "", normalFont, alt);
                addPdfDataCell(memberTable, m.getUser().getEmail() != null ? m.getUser().getEmail() : "", normalFont, alt);
                addPdfDataCell(memberTable, "OWNER".equals(m.getRole()) ? "Chủ phòng" : "Thành viên", normalFont, alt);
            }
            doc.add(memberTable);

            // Category stats
            addSectionHeader(doc, sectionFont, "TỔNG HỢP THEO DANH MỤC");
            PdfPTable statTable = new PdfPTable(new float[]{3, 2, 1, 1});
            statTable.setWidthPercentage(100);
            statTable.setSpacingAfter(12);
            for (String h : new String[]{"Danh mục", "Tổng chi", "Đơn vị", "Tỉ lệ"}) addPdfHeaderCell(statTable, h, boldFont);
            BigDecimal grandTotal = ctx.stats().stream().map(st -> st.getTotalAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            int ri = 1;
            for (var st : ctx.stats()) {
                boolean alt = ri % 2 == 0;
                addPdfDataCell(statTable, st.getCategoryName(), normalFont, alt);
                addPdfDataCell(statTable, formatMoney(st.getTotalAmount()), normalFont, alt);
                addPdfDataCell(statTable, ctx.trip().getCurrency(), normalFont, alt);
                double pct = grandTotal.compareTo(BigDecimal.ZERO) > 0 ? st.getTotalAmount().doubleValue() / grandTotal.doubleValue() * 100 : 0;
                addPdfDataCell(statTable, String.format("%.1f%%", pct), normalFont, alt);
                ri++;
            }
            PdfPCell totalLabel = new PdfPCell(new Phrase("TỔNG CỘNG", boldFont));
            totalLabel.setColspan(1); totalLabel.setPadding(6); totalLabel.setBackgroundColor(new Color(220, 237, 255));
            statTable.addCell(totalLabel);
            PdfPCell totalVal = new PdfPCell(new Phrase(formatMoney(grandTotal) + " " + ctx.trip().getCurrency(), boldFont));
            totalVal.setColspan(3); totalVal.setPadding(6); totalVal.setBackgroundColor(new Color(220, 237, 255));
            statTable.addCell(totalVal);
            doc.add(statTable);

            // Details (optional)
            if (includeDetails) {
                List<Expense> expenses = expenseRepository.findAllByTripIdWithPayerAndCategory(tripId);
                addSectionHeader(doc, sectionFont, "CHI TIẾT TỪNG KHOẢN CHI");
                PdfPTable detTable = new PdfPTable(new float[]{2, 3, 2, 2, 2});
                detTable.setWidthPercentage(100);
                detTable.setSpacingAfter(12);
                for (String h : new String[]{"Ngày", "Tên khoản", "Danh mục", "Người trả", "Số tiền"}) addPdfHeaderCell(detTable, h, boldFont);
                int di = 1;
                for (Expense e : expenses) {
                    boolean alt = di % 2 == 0;
                    addPdfDataCell(detTable, e.getExpenseDate() != null ? e.getExpenseDate().format(DATE_FMT) : "", normalFont, alt);
                    addPdfDataCell(detTable, e.getDescription() != null ? e.getDescription() : "", normalFont, alt);
                    addPdfDataCell(detTable, e.getCategory() != null ? e.getCategory().getName() : "", normalFont, alt);
                    addPdfDataCell(detTable, e.getPayer() != null ? e.getPayer().getName() : "", normalFont, alt);
                    addPdfDataCell(detTable, formatMoney(e.getTotalAmount()), normalFont, alt);
                    di++;
                }
                doc.add(detTable);
            }

            // Settlement (optional)
            if (includeSettlement) {
                List<SettlementResponse> debts = settlementService.calculateSettlement(tripId, userId);
                addSectionHeader(doc, sectionFont, "BẢNG QUYẾT TOÁN NỢ");
                if (debts.isEmpty()) {
                    doc.add(new Paragraph("Không có khoản nợ nào cần quyết toán!", normalFont));
                } else {
                    PdfPTable settleTable = new PdfPTable(new float[]{3, 1, 3, 2});
                    settleTable.setWidthPercentage(100);
                    for (String h : new String[]{"Người nợ", "→", "Người nhận", "Số tiền"}) addPdfHeaderCell(settleTable, h, boldFont);
                    int si = 1;
                    for (SettlementResponse d : debts) {
                        boolean alt = si % 2 == 0;
                        addPdfDataCell(settleTable, d.getFromUserName(), normalFont, alt);
                        addPdfDataCell(settleTable, "→", normalFont, alt);
                        addPdfDataCell(settleTable, d.getToUserName(), normalFont, alt);
                        addPdfDataCell(settleTable, formatMoney(d.getAmount()), normalFont, alt);
                        si++;
                    }
                    doc.add(settleTable);
                }
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF: ", e);
            throw new BusinessException("Lỗi khi xuất file PDF");
        }
    }

    // ─────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────
    private void validateAccess(Long tripId, Long userId) {
        if (!tripMemberRepository.existsByTripIdAndUserId(tripId, userId))
            throw new BusinessException("Access denied: not a member of this trip");
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0";
        return String.format("%,.0f", value.doubleValue());
    }

    private void addSectionHeader(Document doc, Font sectionFont, String text) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100); t.setSpacingBefore(12); t.setSpacingAfter(6);
        PdfPCell cell = new PdfPCell(new Phrase(text, sectionFont));
        cell.setBackgroundColor(COLOR_PRIMARY); cell.setPadding(8); cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        doc.add(t);
    }

    private void addPdfHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(187, 222, 251));
        cell.setPadding(6); cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addPdfDataCell(PdfPTable table, String text, Font font, boolean alt) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        if (alt) cell.setBackgroundColor(COLOR_ROW_ALT);
        cell.setPadding(5);
        table.addCell(cell);
    }
}
