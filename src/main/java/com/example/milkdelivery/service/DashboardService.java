package com.example.milkdelivery.service;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> getDashboardStats(Long milkmanId);
}
