package com.example.milkdelivery.scheduler;

import com.example.milkdelivery.entity.Customer;
import com.example.milkdelivery.entity.DeliverySession;
import com.example.milkdelivery.repository.CustomerRepository;
import com.example.milkdelivery.repository.DeliverySessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DeliverySessionScheduler {

    @Autowired
    private DeliverySessionRepository deliverySessionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // Every 10 seconds (testing) -> @Scheduled(fixedRate = 10000)
    // Run every 5 minutes -> @Scheduled(fixedRate = 300000)
    @Scheduled(fixedRate = 10000)
    public void resetDeliveryCycle() {

        System.out.println(
                "Checking active delivery sessions..."
        );

        List<DeliverySession> sessions =
                deliverySessionRepository.findAll();

        for (DeliverySession session : sessions) {

            // Only active sessions
            if (session.getActive()) {

                System.out.println(
                        "Active session found"
                );

                LocalDateTime startTime =
                        session.getStartedAt();

                LocalDateTime now =
                        LocalDateTime.now();

                // TESTING: 1 minute
                // PRODUCTION: plusHours(12) / plusMinutes(1)
                if (startTime.plusMinutes(1)
                        .isBefore(now)) {

                    System.out.println(
                            "Delivery cycle completed"
                    );

                    // Get all customers
                    List<Customer> customers =

                            customerRepository
                                    .findByUser_Id(
                                            session.getUser().getId()
                                    );

                    for (Customer customer : customers) {

                        // Reset delivery
                        customer.setDeliveryCompleted(false);

                        // Pause logic
                        if (customer.getIsPaused()
                                &&
                                customer.getPauseDays() > 0) {

                            customer.setPauseDays(
                                    customer.getPauseDays() - 1
                            );

                            // Pause completed
                            if (customer.getPauseDays() == 0) {

                                customer.setIsPaused(false);

                                customer.setPauseStartDate(null);

                                customer.setPauseEndDate(null);
                            }
                        }

                        // Extra milk logic
                        if (customer.getExtraMilkDays() > 0) {

                            customer.setExtraMilkDays(
                                    customer.getExtraMilkDays() - 1
                            );

                            // Extra milk completed
                            if (customer.getExtraMilkDays() == 0) {

                                customer.setExtraMilk(0.0);
                            }
                        }

                        customerRepository.save(customer);
                    }

                    // End session
                    session.setActive(false);

                    session.setEndedAt(now);

                    deliverySessionRepository.save(session);
                }
            }
        }
    }
}