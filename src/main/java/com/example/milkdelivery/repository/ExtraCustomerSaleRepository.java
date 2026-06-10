package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.ExtraCustomerSale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ExtraCustomerSaleRepository extends JpaRepository<ExtraCustomerSale, Long> {
    List<ExtraCustomerSale> findByUserIdAndSaleDate(Long userId, LocalDate saleDate);
    List<ExtraCustomerSale> findByUserIdAndSaleDateBetween(Long userId, LocalDate start, LocalDate end);
}
