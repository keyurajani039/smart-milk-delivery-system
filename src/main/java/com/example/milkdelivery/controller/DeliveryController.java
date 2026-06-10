package com.example.milkdelivery.controller;

import com.example.milkdelivery.dto.DeliveryDto;
import com.example.milkdelivery.dto.MonthlySummaryDto;
import com.example.milkdelivery.entity.Delivery;
import com.example.milkdelivery.entity.DeliverySession;
import com.example.milkdelivery.service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @PostMapping("/save")
    public Delivery saveDelivery(@RequestBody Delivery delivery) {
        return deliveryService.saveDelivery(delivery);
    }

    @GetMapping
    public List<Delivery> getAllDeliveries() {
        return deliveryService.getAllDeliveries();
    }

    @GetMapping("/{id}")
    public Delivery getDeliveryById(@PathVariable Long id) {
        return deliveryService.getDeliveryById(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<DeliveryDto> getDeliveriesByCustomer(@PathVariable Long customerId) {
        return deliveryService.getDeliveriesByCustomer(customerId);
    }

    @GetMapping("/today")
    public List<DeliveryDto> getTodayDeliveries() {
        return deliveryService.getTodayDeliveries();
    }

    @GetMapping("/today/milkman/{userId}")
    public List<DeliveryDto> getTodayDeliveriesByMilkman(@PathVariable Long userId) {
        return deliveryService.getTodayDeliveriesByMilkman(userId);
    }

    @PostMapping("/skip-today")
    public ResponseEntity<String> skipTodayDeliveries(@RequestBody Map<String, Long> body) {
        Long milkmanId = body.get("milkmanId");
        if (milkmanId == null) {
            throw new RuntimeException("milkmanId is required in request body");
        }
        String response = deliveryService.skipTodayDeliveries(milkmanId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start-session")
    public ResponseEntity<DeliverySession> startSession(@RequestBody Map<String, Object> body) {
        Long milkmanId = Long.valueOf(body.get("milkmanId").toString());
        Double loadedMilk = Double.valueOf(body.get("loadedMilk").toString());
        DeliverySession session = deliveryService.startSession(milkmanId, loadedMilk);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/session-summary/{milkmanId}")
    public ResponseEntity<Map<String, Object>> getSessionSummary(@PathVariable Long milkmanId) {
        Map<String, Object> summary = deliveryService.getSessionSummary(milkmanId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/monthly")
    public List<DeliveryDto> getMonthlyDeliveries(
            @RequestParam Long customerId,
            @RequestParam int year,
            @RequestParam int month) {
        return deliveryService.getMonthlyDeliveries(customerId, year, month);
    }

    @GetMapping("/monthly-summary")
    public MonthlySummaryDto getMonthlySummary(
            @RequestParam Long customerId,
            @RequestParam int year,
            @RequestParam int month) {
        return deliveryService.getMonthlySummary(customerId, year, month);
    }
}