package com.example.milkdelivery.scheduler;

import com.example.milkdelivery.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoBillScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AutoBillScheduler.class);

    @Autowired
    private PaymentService paymentService;

    // Run automatically on the 1st day of every month at midnight
    @Scheduled(cron = "0 0 0 1 * ?")
    public void runMonthlyBillGeneration() {
        logger.info("Executing scheduled monthly billing cycle...");
        try {
            paymentService.runAutoMonthlyBilling();
            logger.info("Scheduled monthly billing cycle complete.");
        } catch (Exception e) {
            logger.error("Error running monthly billing scheduler: {}", e.getMessage());
        }
    }
}
