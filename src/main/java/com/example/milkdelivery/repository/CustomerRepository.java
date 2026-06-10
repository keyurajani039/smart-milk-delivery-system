package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    List<Customer> findByUser_Id(Long userId);

    List<Customer> findByUser_IdAndActive(Long userId, Boolean active);

    List<Customer> findByUser_IdAndActiveTrue(Long userId);

    Optional<Customer> findByPhoneNumber(String phoneNumber);
}