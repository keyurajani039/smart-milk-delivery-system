package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.DeliverySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliverySessionRepository
        extends JpaRepository<DeliverySession, Long> {

    Optional<DeliverySession>
    findByUser_IdAndActiveTrue(Long userId);
}