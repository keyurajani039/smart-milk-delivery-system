package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.PlanCancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanCancellationRepository extends JpaRepository<PlanCancellation, Long> {
    List<PlanCancellation> findByCustomerId(Long customerId);
}
