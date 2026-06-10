package com.example.milkdelivery.controller;

import com.example.milkdelivery.security.UserDetailsImpl;
import com.example.milkdelivery.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/daily")
    public ResponseEntity<byte[]> getDailyReport(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "pdf") String format) {

        Long milkmanId = userDetails.getUser().getId();
        LocalDate localDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        byte[] data = reportService.generateDailyReport(milkmanId, localDate, format);

        return buildResponse(data, "daily-report-" + localDate, format);
    }

    @GetMapping("/monthly")
    public ResponseEntity<byte[]> getMonthlyReport(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "pdf") String format) {

        Long milkmanId = userDetails.getUser().getId();
        byte[] data = reportService.generateMonthlyReport(milkmanId, month, year, format);

        return buildResponse(data, "monthly-report-" + month + "-" + year, format);
    }

    @GetMapping("/yearly")
    public ResponseEntity<byte[]> getYearlyReport(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam int year,
            @RequestParam(defaultValue = "pdf") String format) {

        Long milkmanId = userDetails.getUser().getId();
        byte[] data = reportService.generateYearlyReport(milkmanId, year, format);

        return buildResponse(data, "yearly-report-" + year, format);
    }

    private ResponseEntity<byte[]> buildResponse(byte[] data, String filename, String format) {
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType;

        if ("excel".equalsIgnoreCase(format)) {
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            headers.setContentDispositionFormData("attachment", filename + ".xlsx");
        } else if ("pdf".equalsIgnoreCase(format)) {
            mediaType = MediaType.APPLICATION_PDF;
            headers.setContentDispositionFormData("attachment", filename + ".pdf");
        } else {
            mediaType = MediaType.TEXT_PLAIN;
            headers.setContentDispositionFormData("attachment", filename + ".csv");
        }

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(data);
    }
}
