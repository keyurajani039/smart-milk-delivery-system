package com.example.milkdelivery.controller;

import com.example.milkdelivery.entity.SubscriptionPlan;
import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.enums.Role;
import com.example.milkdelivery.exception.ResourceNotFoundException;
import com.example.milkdelivery.repository.SubscriptionPlanRepository;
import com.example.milkdelivery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/super-admin")
public class SuperAdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @GetMapping("/milkmen")
    public ResponseEntity<List<User>> listAllMilkmen() {
        List<User> milkmen = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.DELIVERY_MAN)
                .toList();
        return ResponseEntity.ok(milkmen);
    }

    @PutMapping("/milkmen/{id}/block")
    public ResponseEntity<User> toggleBlockStatus(@PathVariable Long id, @RequestParam Boolean block) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setBlocked(block);
        User saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/plans")
    public ResponseEntity<SubscriptionPlan> saveOrUpdatePlan(@RequestBody SubscriptionPlan plan) {
        SubscriptionPlan saved = planRepository.save(plan);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/milkmen/{id}/extend")
    public ResponseEntity<User> extendSubscription(@PathVariable Long id, @RequestParam int days) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDateTime base = user.getSubscriptionEndDate() != null ? user.getSubscriptionEndDate() : LocalDateTime.now();
        user.setSubscriptionEndDate(base.plusDays(days));
        User saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getSaaSAnalytics() {
        List<User> milkmen = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.DELIVERY_MAN)
                .toList();

        long totalMilkmen = milkmen.size();
        long activeTrials = milkmen.stream()
                .filter(u -> u.getTrialEndDate() != null && u.getTrialEndDate().isAfter(LocalDateTime.now()))
                .count();

        long activeSubscriptions = milkmen.stream()
                .filter(u -> u.getSubscriptionEndDate() != null && u.getSubscriptionEndDate().isAfter(LocalDateTime.now()))
                .count();

        long blockedMilkmen = milkmen.stream()
                .filter(u -> Boolean.TRUE.equals(u.getBlocked()))
                .count();

        // Calculate theoretical MRR based on plans
        List<SubscriptionPlan> plans = planRepository.findAll();
        double theoreticalMRR = 0.0;
        for (User u : milkmen) {
            if (u.getSubscriptionPlanId() != null) {
                Long planId = u.getSubscriptionPlanId();
                Optional<SubscriptionPlan> plan = plans.stream().filter(p -> p.getId().equals(planId)).findFirst();
                if (plan.isPresent() && plan.get().getPrice() > 0) {
                    theoreticalMRR += plan.get().getPrice() / (plan.get().getDurationDays() / 30.0);
                }
            }
        }

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalMilkmen", totalMilkmen);
        analytics.put("activeTrials", activeTrials);
        analytics.put("activeSubscriptions", activeSubscriptions);
        analytics.put("blockedMilkmen", blockedMilkmen);
        analytics.put("estimatedMRR", theoreticalMRR);

        return ResponseEntity.ok(analytics);
    }
}
