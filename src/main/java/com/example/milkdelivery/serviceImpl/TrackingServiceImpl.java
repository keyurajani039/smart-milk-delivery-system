package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.entity.Tracking;
import com.example.milkdelivery.repository.TrackingRepository;
import com.example.milkdelivery.service.TrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class TrackingServiceImpl implements TrackingService {

    @Autowired
    private TrackingRepository trackingRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    @CachePut(value = "tracking", key = "#userId")
    public Tracking updateLocation(Long userId, Double latitude, Double longitude, Double speed) {
        Tracking tracking = Tracking.builder()
                .userId(userId)
                .latitude(latitude)
                .longitude(longitude)
                .speed(speed != null ? speed : 0.0)
                .timestamp(LocalDateTime.now())
                .build();

        Tracking saved = trackingRepository.save(tracking);

        // Broadcast to WebSocket clients subscribing to /topic/live-location/{userId}
        messagingTemplate.convertAndSend("/topic/live-location/" + userId, saved);

        // Also broadcast an ETA update to /topic/live-route/{userId} for demonstration
        MapDto routeDto = new MapDto(latitude, longitude, "ETA is calculated dynamically based on location updates");
        messagingTemplate.convertAndSend("/topic/live-route/" + userId, routeDto);

        return saved;
    }

    @Override
    @Cacheable(value = "tracking", key = "#userId", unless = "#result == null")
    public Tracking getCurrentLocation(Long userId) {
        return trackingRepository.findFirstByUserIdOrderByTimestampDesc(userId)
                .orElse(null);
    }

    // Small helper class for route updates
    public static class MapDto {
        public Double latitude;
        public Double longitude;
        public String message;

        public MapDto(Double latitude, Double longitude, String message) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.message = message;
        }
    }
}
