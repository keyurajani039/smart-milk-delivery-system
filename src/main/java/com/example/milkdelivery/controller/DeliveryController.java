package com.example.milkdelivery.controller;

import com.example.milkdelivery.dto.DeliveryDto;
import com.example.milkdelivery.dto.MonthlySummaryDto;
import com.example.milkdelivery.entity.Delivery;
import com.example.milkdelivery.service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @PostMapping("/save")
    public Delivery saveDelivery(
            @RequestBody Delivery delivery) {

        return deliveryService.saveDelivery(delivery);
    }

    @GetMapping
    public List<Delivery> getAllDeliveries() {

        return deliveryService.getAllDeliveries();
    }

    @GetMapping("/{id}")
    public Delivery getDeliveryById(
            @PathVariable Long id) {

        return deliveryService.getDeliveryById(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<DeliveryDto> getDeliveriesByCustomer(
            @PathVariable Long customerId) {

        return deliveryService.getDeliveriesByCustomer(customerId);
    }

    @GetMapping("/today")
    public List<DeliveryDto> getTodayDeliveries() {

        return deliveryService.getTodayDeliveries();
    }

    @GetMapping("/monthly")
    public List<DeliveryDto> getMonthlyDeliveries(

            @RequestParam Long customerId,

            @RequestParam int year,

            @RequestParam int month) {

        return deliveryService
                .getMonthlyDeliveries(
                        customerId,
                        year,
                        month
                );
    }

    @GetMapping("/monthly-summary")
    public MonthlySummaryDto getMonthlySummary(

            @RequestParam Long customerId,

            @RequestParam int year,

            @RequestParam int month) {

        return deliveryService.getMonthlySummary(
                customerId,
                year,
                month
        );
    }
}