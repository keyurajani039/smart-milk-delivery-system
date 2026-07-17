package com.example.milkdelivery.service;

public interface TelegramService {
    void sendAbsenceNotification(String telegramId, String customerName, String companyName);
    void sendBillGeneratedNotification(String telegramId, String customerName, double amount, int month, int year);
    void sendPaymentReceivedNotification(String telegramId, String customerName, double amount);
    void sendPaymentReminderNotification(String telegramId, String customerName, double amount);
    void sendWelcomeNotification(String telegramId, String customerName);
    void sendOtpNotification(String telegramId, String otpCode);
}
