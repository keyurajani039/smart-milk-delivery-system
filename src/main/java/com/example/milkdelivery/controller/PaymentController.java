package com.example.milkdelivery.controller;

import com.example.milkdelivery.dto.PaymentDto;
import com.example.milkdelivery.entity.Payment;
import com.example.milkdelivery.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/save")
    public Payment savePayment(
            @RequestBody Payment payment) {

        return paymentService.savePayment(
                payment
        );
    }

    @GetMapping("/customer/{customerId}")
    public List<PaymentDto> getPaymentsByCustomer(
            @PathVariable Long customerId) {

        return paymentService
                .getPaymentsByCustomer(
                        customerId
                );
    }

    @PutMapping("/mark-paid/{paymentId}")
    public String markPaymentPaid(
            @PathVariable Long paymentId) {

        return paymentService
                .markPaymentPaid(
                        paymentId
                );
    }
}