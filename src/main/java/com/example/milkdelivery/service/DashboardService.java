package com.example.milkdelivery.service;

import java.util.List;
import java.util.Map;

public interface DashboardService {
    Map<String, Object> getDashboardStats(Long milkmanId);
    List<Map<String, Object>> getWeeklyStats(Long milkmanId);
}
