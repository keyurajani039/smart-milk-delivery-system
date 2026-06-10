package com.example.milkdelivery.controller;

import com.example.milkdelivery.entity.RouteDetail;
import com.example.milkdelivery.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @GetMapping("/today/{userId}")
    public ResponseEntity<List<RouteDetail>> getTodayRoute(@PathVariable Long userId) {
        List<RouteDetail> details = routeService.getTodayRoute(userId);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/optimized/{userId}")
    public ResponseEntity<List<RouteDetail>> getOptimizedRoute(@PathVariable Long userId) {
        List<RouteDetail> details = routeService.getOptimizedRoute(userId);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/generate/{userId}")
    public ResponseEntity<String> forceGenerateRoute(@PathVariable Long userId) {
        routeService.generateDailyRoute(userId);
        return ResponseEntity.ok("Daily route generated successfully");
    }
}
