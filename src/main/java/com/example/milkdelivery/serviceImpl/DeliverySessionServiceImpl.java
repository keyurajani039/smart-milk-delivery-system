package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.entity.DeliverySession;
import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.repository.DeliverySessionRepository;
import com.example.milkdelivery.repository.UserRepository;
import com.example.milkdelivery.service.DeliverySessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DeliverySessionServiceImpl
        implements DeliverySessionService {

    @Autowired
    private DeliverySessionRepository deliverySessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public String startDelivery(Long userId) {

        // Check existing active session
        Optional<DeliverySession> existingSession =

                deliverySessionRepository
                        .findByUser_IdAndActiveTrue(userId);

        if (existingSession.isPresent()) {

            return "Delivery session already active";
        }

        // Find milkman
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        ));

        // Create new session
        DeliverySession session =
                DeliverySession.builder()

                        .user(user)

                        .startedAt(LocalDateTime.now())

                        .active(true)

                        .loadedMilkQuantity(0.0)

                        .build();

        deliverySessionRepository.save(session);

        return "Delivery started successfully";
    }
}