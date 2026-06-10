package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.entity.Delivery;
import com.example.milkdelivery.entity.ExtraCustomerSale;
import com.example.milkdelivery.repository.DeliveryRepository;
import com.example.milkdelivery.repository.ExtraCustomerSaleRepository;
import com.example.milkdelivery.service.ReportService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private ExtraCustomerSaleRepository extraSaleRepository;

    @Override
    public byte[] generateDailyReport(Long milkmanId, LocalDate date, String format) {
        List<Delivery> deliveries = deliveryRepository.findByUser_IdAndDeliveryDate(milkmanId, date);
        List<ExtraCustomerSale> extraSales = extraSaleRepository.findByUserIdAndSaleDate(milkmanId, date);

        if ("excel".equalsIgnoreCase(format)) {
            return generateExcelReport("Daily Report - " + date, deliveries, extraSales);
        } else if ("pdf".equalsIgnoreCase(format)) {
            return generatePdfReport("Daily Delivery Report - " + date, deliveries, extraSales);
        } else {
            return generateCsvReport(deliveries, extraSales);
        }
    }

    @Override
    public byte[] generateMonthlyReport(Long milkmanId, int month, int year, String format) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Delivery> deliveries = deliveryRepository.findByCustomer_IdAndDeliveryDateBetween(null, start, end).stream()
                .filter(d -> d.getUser().getId().equals(milkmanId))
                .toList();

        List<ExtraCustomerSale> extraSales = extraSaleRepository.findByUserIdAndSaleDateBetween(milkmanId, start, end);

        if ("excel".equalsIgnoreCase(format)) {
            return generateExcelReport("Monthly Report - " + month + "/" + year, deliveries, extraSales);
        } else if ("pdf".equalsIgnoreCase(format)) {
            return generatePdfReport("Monthly Delivery Report - " + month + "/" + year, deliveries, extraSales);
        } else {
            return generateCsvReport(deliveries, extraSales);
        }
    }

    @Override
    public byte[] generateYearlyReport(Long milkmanId, int year, String format) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        List<Delivery> deliveries = deliveryRepository.findByCustomer_IdAndDeliveryDateBetween(null, start, end).stream()
                .filter(d -> d.getUser().getId().equals(milkmanId))
                .toList();

        List<ExtraCustomerSale> extraSales = extraSaleRepository.findByUserIdAndSaleDateBetween(milkmanId, start, end);

        if ("excel".equalsIgnoreCase(format)) {
            return generateExcelReport("Yearly Report - " + year, deliveries, extraSales);
        } else if ("pdf".equalsIgnoreCase(format)) {
            return generatePdfReport("Yearly Delivery Report - " + year, deliveries, extraSales);
        } else {
            return generateCsvReport(deliveries, extraSales);
        }
    }

    // Excel Export implementation using Apache POI
    private byte[] generateExcelReport(String title, List<Delivery> deliveries, List<ExtraCustomerSale> extraSales) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Deliveries");

            // Title Row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Table Headers
            String[] headers = {"Delivery ID", "Customer Name", "Date", "Regular Qty (L)", "Extra Qty (L)", "Total Qty (L)", "Status"};
            Row headerRow = sheet.createRow(2);
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            for (Delivery d : deliveries) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(d.getId());
                row.createCell(1).setCellValue(d.getCustomer().getCustomerName());
                row.createCell(2).setCellValue(d.getDeliveryDate().toString());
                row.createCell(3).setCellValue(d.getMilkQuantity());
                row.createCell(4).setCellValue(d.getExtraMilk());
                row.createCell(5).setCellValue(d.getTotalMilk());
                row.createCell(6).setCellValue(d.getDeliveryStatus());
            }

            // Extra Customer Sales Sheet
            Sheet sheetSales = workbook.createSheet("Extra Walk-up Sales");
            Row salesHeaderRow = sheetSales.createRow(0);
            String[] salesHeaders = {"ID", "Date", "Liters Sold", "Amount (₹)", "Payment Method", "Notes"};
            for (int i = 0; i < salesHeaders.length; i++) {
                Cell cell = salesHeaderRow.createCell(i);
                cell.setCellValue(salesHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int salesRowIdx = 1;
            for (ExtraCustomerSale s : extraSales) {
                Row row = sheetSales.createRow(salesRowIdx++);
                row.createCell(0).setCellValue(s.getId());
                row.createCell(1).setCellValue(s.getSaleDate().toString());
                row.createCell(2).setCellValue(s.getQuantityLiters());
                row.createCell(3).setCellValue(s.getAmountCollected());
                row.createCell(4).setCellValue(s.getPaymentType());
                row.createCell(5).setCellValue(s.getNotes() != null ? s.getNotes() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            logger.error("Error creating POI Excel report: {}", e.getMessage());
            return new byte[0];
        }
    }

    // PDF Export implementation using iTextPDF
    private byte[] generatePdfReport(String titleText, List<Delivery> deliveries, List<ExtraCustomerSale> extraSales) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // Report Header
            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph(titleText, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Table of regular deliveries
            com.itextpdf.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
            com.itextpdf.text.Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);

            document.add(new Paragraph("Deliveries Summary:", boldFont));
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);

            String[] tableHeaders = {"Cust Name", "Date", "Reg Qty", "Extra Qty", "Total", "Status"};
            for (String h : tableHeaders) {
                PdfPCell cell = new PdfPCell(new Paragraph(h, boldFont));
                cell.setBackgroundColor(new BaseColor(240, 240, 240));
                table.addCell(cell);
            }

            double totalLiters = 0.0;
            for (Delivery d : deliveries) {
                table.addCell(new PdfPCell(new Paragraph(d.getCustomer().getCustomerName(), regularFont)));
                table.addCell(new PdfPCell(new Paragraph(d.getDeliveryDate().toString(), regularFont)));
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(d.getMilkQuantity()), regularFont)));
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(d.getExtraMilk()), regularFont)));
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(d.getTotalMilk()), regularFont)));
                table.addCell(new PdfPCell(new Paragraph(d.getDeliveryStatus(), regularFont)));
                totalLiters += d.getTotalMilk();
            }
            document.add(table);
            document.add(new Paragraph("\nTotal Volume Delivered: " + totalLiters + " Liters", boldFont));
            document.add(new Paragraph("\n"));

            // Table of extra sales
            if (!extraSales.isEmpty()) {
                document.add(new Paragraph("Extra Walk-up Sales:", boldFont));
                document.add(new Paragraph("\n"));

                PdfPTable salesTable = new PdfPTable(5);
                salesTable.setWidthPercentage(100);

                String[] salesTableHeaders = {"Date", "Qty (L)", "Collected (₹)", "Method", "Notes"};
                for (String h : salesTableHeaders) {
                    PdfPCell cell = new PdfPCell(new Paragraph(h, boldFont));
                    cell.setBackgroundColor(new BaseColor(240, 240, 240));
                    salesTable.addCell(cell);
                }

                double totalExtraRevenue = 0.0;
                for (ExtraCustomerSale s : extraSales) {
                    salesTable.addCell(new PdfPCell(new Paragraph(s.getSaleDate().toString(), regularFont)));
                    salesTable.addCell(new PdfPCell(new Paragraph(String.valueOf(s.getQuantityLiters()), regularFont)));
                    salesTable.addCell(new PdfPCell(new Paragraph("₹" + s.getAmountCollected(), regularFont)));
                    salesTable.addCell(new PdfPCell(new Paragraph(s.getPaymentType(), regularFont)));
                    salesTable.addCell(new PdfPCell(new Paragraph(s.getNotes() != null ? s.getNotes() : "", regularFont)));
                    totalExtraRevenue += s.getAmountCollected();
                }
                document.add(salesTable);
                document.add(new Paragraph("\nTotal Extra Sales Cash collected: ₹" + totalExtraRevenue, boldFont));
            }

            document.close();
        } catch (Exception e) {
            logger.error("Error creating iText PDF report: {}", e.getMessage());
        }
        return out.toByteArray();
    }

    // CSV Export implementation
    private byte[] generateCsvReport(List<Delivery> deliveries, List<ExtraCustomerSale> extraSales) {
        StringBuilder csv = new StringBuilder();
        csv.append("Type,Record ID,Customer/Sales Name,Date,Liters,Amount/Status,Notes\n");

        for (Delivery d : deliveries) {
            csv.append("Delivery,")
                    .append(d.getId()).append(",")
                    .append(d.getCustomer().getCustomerName()).append(",")
                    .append(d.getDeliveryDate()).append(",")
                    .append(d.getTotalMilk()).append(",")
                    .append(d.getDeliveryStatus()).append(",")
                    .append("\n");
        }

        for (ExtraCustomerSale s : extraSales) {
            csv.append("Extra Sale,")
                    .append(s.getId()).append(",")
                    .append("Walk-up Customer").append(",")
                    .append(s.getSaleDate()).append(",")
                    .append(s.getQuantityLiters()).append(",")
                    .append(s.getAmountCollected()).append(",")
                    .append(s.getNotes() != null ? s.getNotes() : "").append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
