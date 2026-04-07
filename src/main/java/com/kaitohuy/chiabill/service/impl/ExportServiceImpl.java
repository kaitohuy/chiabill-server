package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.entity.*;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.repository.*;
import com.kaitohuy.chiabill.service.interfaces.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.FontFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final ExpenseRepository expenseRepository;

    @Override
    public byte[] exportTripToExcel(Long tripId, Long userId) {
        validateAccess(tripId, userId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        List<TripMember> members = tripMemberRepository.findActiveMembersWithUser(tripId);
        // Lấy thống kê theo category
        List<com.kaitohuy.chiabill.dto.response.CategoryStatResponse> stats = expenseRepository.getExpenseStatsByCategory(tripId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bao cao chuyen di");

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            int rowIdx = 0;

            // 1. Title
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO CHI TIẾT CHUYẾN ĐI: " + trip.getName().toUpperCase());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            rowIdx++; // Empty row
            
            // 2. Thông tin chung
            createLabelValueRow(sheet, rowIdx++, "Mô tả:", trip.getDescription() != null ? trip.getDescription() : "N/A");
            createLabelValueRow(sheet, rowIdx++, "Ngân sách:", trip.getTotalBudget() != null ? trip.getTotalBudget().toString() + " " + trip.getCurrency() : "0 " + trip.getCurrency());
            createLabelValueRow(sheet, rowIdx++, "Ngày xuất báo cáo:", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            
            rowIdx += 2;

            // 3. Member Table
            Row memberHeaderRow = sheet.createRow(rowIdx++);
            String[] memberHeaders = {"STT", "Họ tên", "Số điện thoại", "Email", "Vai trò"};
            for (int i = 0; i < memberHeaders.length; i++) {
                Cell cell = memberHeaderRow.createCell(i);
                cell.setCellValue(memberHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int stt = 1;
            for (TripMember member : members) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(member.getUser().getName());
                row.createCell(2).setCellValue(member.getUser().getPhone() != null ? member.getUser().getPhone() : "");
                row.createCell(3).setCellValue(member.getUser().getEmail() != null ? member.getUser().getEmail() : "");
                row.createCell(4).setCellValue("OWNER".equals(member.getRole()) ? "Chủ phòng" : "Thành viên");
            }

            rowIdx += 2;

            // 4. Category Summary Table
            Row expenseHeaderRow = sheet.createRow(rowIdx++);
            String[] expenseHeaders = {"Danh mục", "Tổng chi phí", "Đơn vị"};
            for (int i = 0; i < expenseHeaders.length; i++) {
                Cell cell = expenseHeaderRow.createCell(i);
                cell.setCellValue(expenseHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            BigDecimal grandTotal = BigDecimal.ZERO;
            for (var stat : stats) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(stat.getCategoryName());
                row.createCell(1).setCellValue(stat.getTotalAmount().doubleValue());
                row.createCell(2).setCellValue(trip.getCurrency());
                grandTotal = grandTotal.add(stat.getTotalAmount());
            }

            // Total row
            Row totalRow = sheet.createRow(rowIdx++);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TỔNG CỘNG");
            totalLabel.setCellStyle(headerStyle);
            Cell totalVal = totalRow.createCell(1);
            totalVal.setCellValue(grandTotal.doubleValue());
            totalVal.setCellStyle(headerStyle);

            // Autosize
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Excel: ", e);
            throw new BusinessException("Lỗi khi xuất file Excel");
        }
    }

    @Override
    public byte[] exportTripToPdf(Long tripId, Long userId) {
        validateAccess(tripId, userId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        List<TripMember> members = tripMemberRepository.findActiveMembersWithUser(tripId);
        List<com.kaitohuy.chiabill.dto.response.CategoryStatResponse> stats = expenseRepository.getExpenseStatsByCategory(tripId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // 1. Title
            Paragraph title = new Paragraph("BAO CAO CHUYEN DI: " + trip.getName().toUpperCase(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 2. Info
            document.add(new Paragraph("Mo ta: " + (trip.getDescription() != null ? trip.getDescription() : "N/A"), normalFont));
            document.add(new Paragraph("Ngan sach: " + trip.getTotalBudget() + " " + trip.getCurrency(), normalFont));
            document.add(new Paragraph("Ngay xuat: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont));
            document.add(new Paragraph(" ", normalFont)); // Spacer

            // 3. Member Table
            document.add(new Paragraph("DANH SACH THANH VIEN", headerFont));
            PdfPTable memberTable = new PdfPTable(4);
            memberTable.setWidthPercentage(100);
            memberTable.setSpacingBefore(10);
            addPdfCell(memberTable, "Ho ten", headerFont);
            addPdfCell(memberTable, "SDT", headerFont);
            addPdfCell(memberTable, "Email", headerFont);
            addPdfCell(memberTable, "Vai tro", headerFont);

            for (TripMember m : members) {
                addPdfCell(memberTable, m.getUser().getName(), normalFont);
                addPdfCell(memberTable, m.getUser().getPhone() != null ? m.getUser().getPhone() : "", normalFont);
                addPdfCell(memberTable, m.getUser().getEmail() != null ? m.getUser().getEmail() : "", normalFont);
                addPdfCell(memberTable, "OWNER".equals(m.getRole()) ? "Chu phong" : "Thanh vien", normalFont);
            }
            document.add(memberTable);
            document.add(new Paragraph(" ", normalFont));

            // 4. Expense Table
            document.add(new Paragraph("TONG HOP CHI PHI", headerFont));
            PdfPTable expenseTable = new PdfPTable(2);
            expenseTable.setWidthPercentage(100);
            expenseTable.setSpacingBefore(10);
            addPdfCell(expenseTable, "Danh muc", headerFont);
            addPdfCell(expenseTable, "Tong tien (" + trip.getCurrency() + ")", headerFont);

            BigDecimal grandTotal = BigDecimal.ZERO;
            for (var stat : stats) {
                addPdfCell(expenseTable, stat.getCategoryName(), normalFont);
                addPdfCell(expenseTable, stat.getTotalAmount().toString(), normalFont);
                grandTotal = grandTotal.add(stat.getTotalAmount());
            }
            
            // Total row
            addPdfCell(expenseTable, "TONG CONG", headerFont);
            addPdfCell(expenseTable, grandTotal.toString(), headerFont);
            
            document.add(expenseTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF: ", e);
            throw new BusinessException("Loi khi xuat file PDF");
        }
    }

    private void validateAccess(Long tripId, Long userId) {
        if (!tripMemberRepository.existsByTripIdAndUserId(tripId, userId)) {
            throw new BusinessException("Access denied: not a member of this trip");
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void createLabelValueRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private void addPdfCell(PdfPTable table, String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }
}
