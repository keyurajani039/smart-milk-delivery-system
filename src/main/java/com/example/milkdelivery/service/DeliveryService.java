package com.example.milkdelivery.service;

import com.example.milkdelivery.dto.DeliveryDto;
import com.example.milkdelivery.entity.Delivery;

import java.util.List;

public interface DeliveryService {

    Delivery saveDelivery(Delivery delivery);

    List<Delivery> getAllDeliveries();

    Delivery getDeliveryById(Long id);

    List<DeliveryDto> getDeliveriesByCustomer(Long customerId);

    List<DeliveryDto> getTodayDeliveries();

    List<DeliveryDto> getMonthlyDeliveries(
            Long customerId,
            int year,
            int month
    );
}