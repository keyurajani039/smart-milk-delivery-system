package com.example.milkdelivery.controller;

import com.example.milkdelivery.dto.PaymentDto;
import com.example.milkdelivery.entity.Payment;
import com.example.milkdelivery.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/save")
    public Payment savePayment(@RequestBody Payment payment) {
        return paymentService.savePayment(payment);
    }

    @PostMapping("/generate-bill")
    public ResponseEntity<Payment> generateBill(@RequestBody Map<String, Object> body) {
        Long customerId = Long.valueOf(body.get("customerId").toString());
        int month = Integer.parseInt(body.get("month").toString());
        int year = Integer.parseInt(body.get("year").toString());

        Payment payment = paymentService.generateBill(customerId, month, year);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/customer/{customerId}")
    public List<PaymentDto> getPaymentsByCustomer(@PathVariable Long customerId) {
        return paymentService.getPaymentsByCustomer(customerId);
    }

    @PutMapping("/mark-paid/{paymentId}")
    public String markPaymentPaid(
            @PathVariable Long paymentId,
            @RequestBody PaymentDto paymentDto) {
        return paymentService.markPaymentPaid(paymentId, paymentDto);
    }

    @GetMapping(value = "/invoice/{paymentId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long paymentId) {
        byte[] pdfBytes = paymentService.generateInvoicePdf(paymentId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "invoice-" + paymentId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @GetMapping("/qr/{paymentId}")
    public ResponseEntity<Map<String, String>> getPaymentQrCode(@PathVariable Long paymentId) {
        String base64Qr = paymentService.generateUpiQrCode(paymentId);
        return ResponseEntity.ok(Map.of(
                "paymentId", String.valueOf(paymentId),
                "qrBase64", base64Qr,
                "format", "image/png"
        ));
    }
}