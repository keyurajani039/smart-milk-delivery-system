package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    List<Payment> findByCustomer_Id(Long customerId);

    List<Payment> findByMonthAndYear(
            Integer month,
            Integer year
    );
}