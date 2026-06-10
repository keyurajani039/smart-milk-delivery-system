package com.example.milkdelivery.controller;

import com.example.milkdelivery.entity.Tracking;
import com.example.milkdelivery.service.TrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    @Autowired
    private TrackingService trackingService;

    @PostMapping("/update")
    public ResponseEntity<Tracking> updateLocation(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Double latitude = Double.valueOf(body.get("latitude").toString());
        Double longitude = Double.valueOf(body.get("longitude").toString());
        Double speed = body.containsKey("speed") ? Double.valueOf(body.get("speed").toString()) : 0.0;

        Tracking updated = trackingService.updateLocation(userId, latitude, longitude, speed);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/current/{userId}")
    public ResponseEntity<Tracking> getCurrentLocation(@PathVariable Long userId) {
        Tracking current = trackingService.getCurrentLocation(userId);
        if (current == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(current);
    }
}
