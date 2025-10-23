package com.microservices.gateway.controller;

import com.microservices.gateway.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/risk-distribution")
    public ResponseEntity<Map<String, Object>> getRiskDistribution() {
        return ResponseEntity.ok(analyticsService.getRiskDistribution());
    }

    @GetMapping("/payment-trends")
    public ResponseEntity<Map<String, Object>> getPaymentTrends() {
        return ResponseEntity.ok(analyticsService.getPaymentTrends());
    }

    @GetMapping("/response-times")
    public ResponseEntity<Map<String, Object>> getResponseTimes() {
        return ResponseEntity.ok(analyticsService.getResponseTimes());
    }

    @GetMapping("/system-health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        return ResponseEntity.ok(analyticsService.getSystemHealth());
    }
}