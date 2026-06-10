package com.example.milkdelivery.service;

import com.example.milkdelivery.dto.PaymentDto;
import com.example.milkdelivery.entity.Payment;

import java.util.List;

public interface PaymentService {

    Payment savePayment(Payment payment);

    List<PaymentDto> getPaymentsByCustomer(Long customerId);

    String markPaymentPaid(Long paymentId, PaymentDto paymentDto);

    Payment generateBill(Long customerId, int month, int year);

    byte[] generateInvoicePdf(Long paymentId);

    String generateUpiQrCode(Long paymentId);

    void runAutoMonthlyBilling();
}