package com.example.milkdelivery.service;

import java.time.LocalDate;

public interface ReportService {
    byte[] generateDailyReport(Long milkmanId, LocalDate date, String format);
    byte[] generateMonthlyReport(Long milkmanId, int month, int year, String format);
    byte[] generateYearlyReport(Long milkmanId, int year, String format);
}
