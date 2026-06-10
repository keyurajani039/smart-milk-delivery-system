package com.example.milkdelivery.service;

import com.example.milkdelivery.entity.Route;
import com.example.milkdelivery.entity.RouteDetail;

import java.util.List;

public interface RouteService {
    Route generateDailyRoute(Long milkmanId);
    List<RouteDetail> getTodayRoute(Long milkmanId);
    List<RouteDetail> getOptimizedRoute(Long milkmanId);
}
