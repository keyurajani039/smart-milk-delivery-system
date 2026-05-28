package com.example.milkdelivery.controller;

import com.example.milkdelivery.service.DeliverySessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery-session")
public class DeliverySessionController {

    @Autowired
    private DeliverySessionService deliverySessionService;

    @PostMapping("/start/{userId}")
    public String startDelivery(
            @PathVariable Long userId) {

        return deliverySessionService
                .startDelivery(userId);
    }
}