package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.SubscriptionPaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionPaymentLogRepository extends JpaRepository<SubscriptionPaymentLog, Long> {
    List<SubscriptionPaymentLog> findByUser_Id(Long userId);
}
