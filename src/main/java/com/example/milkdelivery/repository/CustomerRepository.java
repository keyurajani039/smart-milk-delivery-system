package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    List<Customer> findByUser_Id(Long userId);

}