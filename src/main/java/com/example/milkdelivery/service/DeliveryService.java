package com.example.milkdelivery.service;

import com.example.milkdelivery.dto.DeliveryDto;
import com.example.milkdelivery.dto.MonthlySummaryDto;
import com.example.milkdelivery.entity.Delivery;
import com.example.milkdelivery.entity.DeliverySession;

import java.util.List;
import java.util.Map;

public interface DeliveryService {

    Delivery saveDelivery(Delivery delivery);

    List<Delivery> getAllDeliveries();

    Delivery getDeliveryById(Long id);

    List<DeliveryDto> getDeliveriesByCustomer(Long customerId);

    List<DeliveryDto> getTodayDeliveries();

    List<DeliveryDto> getMonthlyDeliveries(Long customerId, int year, int month);

    MonthlySummaryDto getMonthlySummary(Long customerId, int year, int month);

    List<DeliveryDto> getTodayDeliveriesByMilkman(Long milkmanId);

    String skipTodayDeliveries(Long milkmanId);

    DeliverySession startSession(Long milkmanId, Double loadedMilk);

    Map<String, Object> getSessionSummary(Long milkmanId);
}