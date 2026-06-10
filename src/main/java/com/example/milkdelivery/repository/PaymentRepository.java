package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    List<Payment> findByCustomer_Id(Long customerId);

    List<Payment> findByMonthAndYear(
            Integer month,
            Integer year
    );

    Optional<Payment> findByCustomerIdAndMonthAndYear(Long customerId, Integer month, Integer year);

    List<Payment> findByCustomer_User_Id(Long userId);

    List<Payment> findByCustomer_User_IdAndMonthAndYear(Long userId, Integer month, Integer year);
}