package com.example.milkdelivery.controller;

import com.example.milkdelivery.security.UserDetailsImpl;
import com.example.milkdelivery.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getStatsByUserId(@PathVariable Long userId) {
        Map<String, Object> stats = dashboardService.getDashboardStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStats(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getUser().getId();
        Map<String, Object> stats = dashboardService.getDashboardStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/weekly/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyStatsByUserId(@PathVariable Long userId) {
        List<Map<String, Object>> stats = dashboardService.getWeeklyStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyStats(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getUser().getId();
        List<Map<String, Object>> stats = dashboardService.getWeeklyStats(userId);
        return ResponseEntity.ok(stats);
    }
}
