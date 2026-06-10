package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.Tracking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface TrackingRepository extends JpaRepository<Tracking, Long> {
    Optional<Tracking> findFirstByUserIdOrderByTimestampDesc(Long userId);
    List<Tracking> findByUserIdOrderByTimestampDesc(Long userId);
}
