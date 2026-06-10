package com.example.milkdelivery.scheduler;

import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.enums.Role;
import com.example.milkdelivery.repository.UserRepository;
import com.example.milkdelivery.service.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RouteScheduler {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(RouteScheduler.class);

    @Autowired
    private RouteService routeService;

    @Autowired
    private UserRepository userRepository;

    // Run every day at 5:00 AM
    @Scheduled(cron = "0 0 5 * * ?")
    public void generateDailyRoutesForAllMilkmen() {
        logger.info("Starting automated daily route generation at 5:00 AM...");
        List<User> milkmen = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.DELIVERY_MAN)
                .toList();

        for (User milkman : milkmen) {
            try {
                routeService.generateDailyRoute(milkman.getId());
                logger.info("Optimized route generated for milkman id: {}", milkman.getId());
            } catch (Exception e) {
                logger.error("Failed to generate optimized route for milkman id {}: {}", milkman.getId(), e.getMessage());
            }
        }
        logger.info("Automated daily route generation complete.");
    }
}
