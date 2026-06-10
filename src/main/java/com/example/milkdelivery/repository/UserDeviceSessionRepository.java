package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.UserDeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserDeviceSessionRepository extends JpaRepository<UserDeviceSession, Long> {
    Optional<UserDeviceSession> findByUserIdAndDeviceId(Long userId, String deviceId);
    List<UserDeviceSession> findByUserId(Long userId);
    void deleteByUserIdAndDeviceId(Long userId, String deviceId);
}
