package com.example.milkdelivery;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RoutingAndSecurityTests {

    @Test
    public void testHaversineDistanceCalculation() {
        // Surat Coordinates
        double lat1 = 21.1702;
        double lon1 = 72.8311;

        // Mumbai Coordinates
        double lat2 = 19.0760;
        double lon2 = 72.8777;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = 6371 * c; // Earth radius in km

        // Distance should be approximately 233 kilometers
        assertTrue(distance > 220 && distance < 250, "Distance calculation is outside the expected bounds of ~233km");
    }

    @Test
    public void testGujaratiNlpParsing() {
        com.example.milkdelivery.serviceImpl.OllamaAiServiceImpl service = new com.example.milkdelivery.serviceImpl.OllamaAiServiceImpl();
        String text = "મને કાલે ૨ લીટર દૂધ વધારે આપજો";
        java.util.Map<String, Object> result = service.parseGujaratiRequest(text);
        
        System.out.println("TEST NLP RESULT: " + result);
        assertEquals("EXTRA_MILK", result.get("action"));
        assertEquals(2.0, (double) result.get("quantity"), 0.01);
    }

    @Test
    public void testGujaratiCancelPlanParsing() {
        com.example.milkdelivery.serviceImpl.OllamaAiServiceImpl service = new com.example.milkdelivery.serviceImpl.OllamaAiServiceImpl();
        String text = "પ્લાન કાયમી બંધ કરો";
        java.util.Map<String, Object> result = service.parseGujaratiRequest(text);
        
        System.out.println("TEST NLP RESULT FOR CANCEL PLAN: " + result);
        assertEquals("CANCEL_PLAN", result.get("action"));
    }

    @Test
    public void testTypoCancelPlanParsing() {
        com.example.milkdelivery.serviceImpl.OllamaAiServiceImpl service = new com.example.milkdelivery.serviceImpl.OllamaAiServiceImpl();
        
        // Test "Palm cancle" typo
        java.util.Map<String, Object> result1 = service.parseGujaratiRequest("Palm cancle");
        assertEquals("CANCEL_PLAN", result1.get("action"));

        // Test "Cancle my plan" typo
        java.util.Map<String, Object> result2 = service.parseGujaratiRequest("Cancle my plan");
        assertEquals("CANCEL_PLAN", result2.get("action"));
    }
}
