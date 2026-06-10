package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DeliveryRepository
        extends JpaRepository<Delivery, Long> {

    List<Delivery> findByCustomer_Id(Long customerId);

    List<Delivery> findByDeliveryDate(LocalDate deliveryDate);

    List<Delivery> findByCustomer_IdAndDeliveryDateBetween(
            Long customerId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Delivery> findByUser_IdAndDeliveryDate(Long userId, LocalDate deliveryDate);
}