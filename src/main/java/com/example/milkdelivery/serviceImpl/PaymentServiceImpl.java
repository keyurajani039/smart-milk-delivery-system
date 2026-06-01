package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.dto.PaymentDto;
import com.example.milkdelivery.entity.Customer;
import com.example.milkdelivery.entity.Payment;
import com.example.milkdelivery.enums.PaymentStatus;
import com.example.milkdelivery.exception.ResourceNotFoundException;
import com.example.milkdelivery.repository.CustomerRepository;
import com.example.milkdelivery.repository.PaymentRepository;
import com.example.milkdelivery.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public Payment savePayment(Payment payment) {

        Customer customer =
                customerRepository.findById(
                        payment.getCustomer().getId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found"
                        ));

        payment.setCustomer(customer);

        return paymentRepository.save(payment);
    }

    @Override
    public List<PaymentDto> getPaymentsByCustomer(
            Long customerId) {

        List<Payment> payments =
                paymentRepository.findByCustomer_Id(
                        customerId
                );

        if (payments.isEmpty()) {

            throw new ResourceNotFoundException(
                    "No payments found"
            );
        }

        return payments.stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public String markPaymentPaid(
            Long paymentId) {

        Payment payment =
                paymentRepository.findById(
                                paymentId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found"
                                ));

        payment.setPaymentStatus(
                PaymentStatus.PAID
        );

        payment.setPaymentDate(
                LocalDate.now()
        );

        paymentRepository.save(payment);

        return "Payment marked as paid successfully";
    }

    private PaymentDto convertToDto(
            Payment payment) {

        return PaymentDto.builder()

                .id(payment.getId())

                .customerId(
                        payment.getCustomer().getId()
                )

                .customerName(
                        payment.getCustomer()
                                .getCustomerName()
                )

                .month(
                        payment.getMonth()
                )

                .year(
                        payment.getYear()
                )

                .amount(
                        payment.getAmount()
                )

                .paymentStatus(
                        payment.getPaymentStatus()
                                .name()
                )

                .paymentDate(
                        payment.getPaymentDate()
                )

                .remarks(
                        payment.getRemarks()
                )

                .build();
    }
}