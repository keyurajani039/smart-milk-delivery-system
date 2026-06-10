package com.example.milkdelivery.service;

import com.example.milkdelivery.entity.Tracking;

public interface TrackingService {
    Tracking updateLocation(Long userId, Double latitude, Double longitude, Double speed);
    Tracking getCurrentLocation(Long userId);
}
